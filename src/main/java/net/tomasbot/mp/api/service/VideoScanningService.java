package net.tomasbot.mp.api.service;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import net.tomasbot.mp.model.Video;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class VideoScanningService implements ConvertFileScanningService<Video> {

  private static final Logger logger = LogManager.getLogger(VideoScanningService.class);

  private final VideoService videoService;
  private final VideoFileScanner videoFileScanner;
  private final TranscodingService transcodingService;
  private final RecursiveWatcherService watcherService;
  private final FileUtilitiesService fileUtilitiesService;
  private final FileTransferWatcher transferWatcher;

  private final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

  @Value("${videos.storage-location}")
  private Path videoStorageLocation;

  VideoScanningService(
      VideoService videoService,
      VideoFileScanner videoFileScanner,
      TranscodingService transcodingService,
      RecursiveWatcherService watcherService,
      FileUtilitiesService fileUtilitiesService,
      FileTransferWatcher transferWatcher) {
    this.videoService = videoService;
    this.videoFileScanner = videoFileScanner;
    this.transcodingService = transcodingService;
    this.watcherService = watcherService;
    this.fileUtilitiesService = fileUtilitiesService;
    this.transferWatcher = transferWatcher;
  }

  private static void handleTranscodeVideoFailed(Video video, @NotNull Path converted) {
    logger.error("Transcoding video failed: {}", video);
    try {
      if (converted.toFile().exists()) {
        Files.delete(converted);
        // todo - delete transcode log, delete converted data, throw error
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public MultiValueMap<String, Path> getInvalidFiles() {
    return new LinkedMultiValueMap<>(invalidFiles).deepCopy();
  }

  @Override
  public void scanFile(@NotNull Path file, @NotNull Collection<Path> existing) {
    try {
      fileUtilitiesService.repairFilename(file);
      logger.trace("Attempting to scan video file: {}", file);
      if (!existing.contains(file)) {
        logger.info("Scanning new video: {}", file);
        createVideo(file);
      } else {
        logger.trace("Video file: {} has already been scanned", file);
      }
    } catch (Throwable e) {
      logger.error("Error scanning video: {}", e.getMessage(), e);
      String ext = FilenameUtils.getExtension(file.toString());
      invalidFiles.add(ext, file);
    }
  }

  private void createVideo(@NotNull Path file) {
    final Video video = new Video(file);
    videoFileScanner.scanFileMetadata(video);
  }

  @Override
  public void scanAddFile(@NotNull Path file) {
    logger.info("Attempting to add new video file: {}", file);
    try {
      fileUtilitiesService.repairFilename(file);
      final Video video = new Video(file);
      if (transcodingService.requiresTranscode(video)) {
        transcodingService.transcodeVideo(
            video,
            (converted) -> finalizeTranscodeVideo(file, converted),
            (converted) -> handleTranscodeVideoFailed(video, converted));
      } else {
        moveVideoToStorage(file);
      }
    } catch (Throwable e) {
      logger.error("Error scanning video file to add: {}", e.getMessage(), e);
    }
  }

  private void finalizeTranscodeVideo(@NotNull Path file, @NotNull Path converted) {
    try {
      if (converted.toFile().exists()) {
        moveVideoToStorage(converted);
        Files.delete(file); // delete original
      } else {
        throw new IOException("Could not locate transcoded video: " + converted);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void moveVideoToStorage(@NotNull Path file) throws IOException {
    final Path filename = file.getFileName();
    final File videoFile = videoStorageLocation.resolve(filename).toFile();
    logger.info("Renaming video file {} to: {}", file, videoFile);
    final boolean renamed = file.toFile().renameTo(videoFile);
    if (!renamed) {
      throw new IOException("Could not move video from 'add' to 'storage'");
    }
  }

  public void handleAddVideoEvent(@NotNull Path path, @NotNull WatchEvent.Kind<?> kind) {
    if (ENTRY_CREATE.equals(kind)) {
      if (Files.isDirectory(path)) {
        watcherService.walkTreeAndSetWatches(
            path, this::scanAddFile, this::handleAddVideoEvent, null);
      } else {
        logger.info("Found video to add: {}", path);
        transferWatcher.watchFileTransfer(path, this::scanAddFile);
      }
    } else if (ENTRY_MODIFY.equals(kind)) {
      transferWatcher.watchFileTransfer(path, this::scanAddFile);
    } else if (ENTRY_DELETE.equals(kind)) {
      logger.info("File deleted from video add directory: {}", path);
    }
  }

  @Override
  public void handleFileEvent(@NotNull Path path, @NotNull WatchEvent.Kind<?> kind) {
    if (ENTRY_CREATE.equals(kind)) {
      if (Files.isDirectory(path)) {
        watcherService.walkTreeAndSetWatches(
            path, dir -> this.scanFile(dir, new ArrayList<>()), this::handleFileEvent, null);
      } else {
        logger.info("Adding new video: {}", path);
        transferWatcher.watchFileTransfer(path, this::createVideo);
      }
    } else if (ENTRY_MODIFY.equals(kind)) {
      transferWatcher.watchFileTransfer(path, this::createVideo);
    } else if (ENTRY_DELETE.equals(kind)) {
      videoService.getVideoByPath(path).forEach(videoService::deleteVideo);
    }
  }
}
