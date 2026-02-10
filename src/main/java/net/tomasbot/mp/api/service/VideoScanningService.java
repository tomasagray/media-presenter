package net.tomasbot.mp.api.service;

import net.tomasbot.mp.api.service.user.UserVideoService;
import net.tomasbot.mp.model.Tag;
import net.tomasbot.mp.model.Video;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class VideoScanningService implements ConvertFileScanningService<Video> {

  private static final Logger logger = LogManager.getLogger(VideoScanningService.class);

  private final VideoService videoService;
  private final UserVideoService userVideoService;
  private final VideoFileScanner videoFileScanner;
  private final TranscodingService transcodingService;
  private final RecursiveWatcherService watcherService;
  private final FileUtilitiesService fileUtilitiesService;
  private final FileTransferWatcher transferWatcher;
  private final InvalidFilesService invalidFilesService;
  private final PathTagService pathTagService;

  private final Map<Path, Set<Tag>> transferTags = new ConcurrentHashMap<>();

  @Value("${videos.add-location}")
  private Path videoAddLocation;

  @Value("${videos.storage-location}")
  private Path videoStorageLocation;

  VideoScanningService(
          VideoService videoService,
          UserVideoService userVideoService,
          VideoFileScanner videoFileScanner,
          TranscodingService transcodingService,
          RecursiveWatcherService watcherService,
          FileUtilitiesService fileUtilitiesService,
          FileTransferWatcher transferWatcher,
          InvalidFilesService invalidFilesService,
          PathTagService pathTagService) {
    this.videoService = videoService;
    this.userVideoService = userVideoService;
    this.videoFileScanner = videoFileScanner;
    this.transcodingService = transcodingService;
    this.watcherService = watcherService;
    this.fileUtilitiesService = fileUtilitiesService;
    this.transferWatcher = transferWatcher;
    this.invalidFilesService = invalidFilesService;
    this.pathTagService = pathTagService;
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
      invalidFilesService.addInvalidFile(file, Video.class);
    }
  }

  private synchronized void createVideo(@NotNull Path file) {
    if (!videoService.getVideoByPath(file).isEmpty()) {
      logger.error("Video already exists at path: {}", file);
      return;
    }

    // apply Tags obtained from "add" directory
    final Video video = new Video(file);
    Set<Tag> addedTags = transferTags.remove(file);
    if (addedTags != null) video.getTags().addAll(addedTags);

    videoFileScanner.scanFileMetadata(video);
  }

  @Override
  public void scanAddFile(@NotNull Path file) {
    logger.info("Attempting to add new video file: {}", file);
    try {
      fileUtilitiesService.repairFilename(file);
      final Video video = new Video(file);

      // set temporary tags
      Path relativized = videoAddLocation.relativize(file);
      List<Tag> tags = pathTagService.getTagsFrom(relativized);
      video.getTags().addAll(tags);

      if (transcodingService.requiresTranscode(video)) {
        transcodingService.transcodeVideo(
            video,
                (converted) -> finalizeTranscodeVideo(video, converted),
                (converted) -> handleTranscodeVideoFailed(video, converted));
      } else {
        moveVideoToStorage(video);
      }
    } catch (Throwable e) {
      logger.error("Error scanning video file to add: {}", e.getMessage(), e);
      invalidFilesService.addInvalidFile(file, Video.class);
    }
  }

  private synchronized void finalizeTranscodeVideo(@NotNull Video video, @NotNull Path converted) {
    try {
      if (converted.toFile().exists()) {
        Video convertedVideo = new Video(converted);
        convertedVideo.getTags().addAll(video.getTags());

        moveVideoToStorage(convertedVideo);

        Path originalVideoFile = video.getFile();
        logger.info("Deleting original video file after transcode: {}", originalVideoFile);
        boolean deleted = originalVideoFile.toFile().delete();
        if (!deleted || originalVideoFile.toFile().exists()) {
          logger.error("Could not delete original video file after transcode: {}", originalVideoFile);
        }
      } else {
        logger.error("Could not locate transcoded video: {}", converted);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void moveVideoToStorage(@NotNull Video video) throws IOException {
    final Path file = video.getFile();
    final Path filename = file.getFileName();

    File storedVideoFile = videoStorageLocation.resolve(filename).toFile();
    // detect collision
    if (storedVideoFile.exists()) {
      storedVideoFile = getMitigatedFilename(storedVideoFile);
    }

    // save temporary tags; will be read in createVideo()
    transferTags.put(storedVideoFile.toPath(), video.getTags());

    logger.info("Renaming video file {} to: {}", file, storedVideoFile);
    final boolean renamed = file.toFile().renameTo(storedVideoFile);
    if (!renamed) {
      throw new IOException("Could not move video from 'add' to 'storage'");
    }
  }

  @NotNull
  private File getMitigatedFilename(@NotNull final File storedVideoFile) {
    final String storedVideoFileName = storedVideoFile.getName();
    final String filename = FilenameUtils.removeExtension(storedVideoFileName);
    final String extension = FilenameUtils.getExtension(storedVideoFileName);

    File mitigated = storedVideoFile;
    for (int i = 1; mitigated.exists(); i++) {
      String mitigatedName = String.format("%s (%d).%s", filename, i, extension);
      mitigated = videoStorageLocation.resolve(mitigatedName).toFile();
    }

    return mitigated;
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
      videoService.getVideoByPath(path).forEach(this::handleDeleteVideo);
    }
  }

  private void handleDeleteVideo(@NotNull Video video) {
    try {
      boolean invalid = invalidFilesService.deleteInvalidFile(video.getFile(), video.getClass());
      if (invalid) logger.info("Deleted invalid video: {}", video);
      userVideoService.unfavoriteForAllUsers(video);
      videoService.deleteVideo(video);
    } catch (IOException e) {
      logger.error("Could not delete {}: {}", video, e.getMessage());
      logger.debug(e);
    }
  }
}
