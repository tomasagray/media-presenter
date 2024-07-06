package net.tomasbot.mp.api.service;

import static java.nio.file.StandardWatchEventKinds.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import net.tomasbot.mp.model.Picture;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class PictureScanningService implements FileScanningService {

  private static final Logger logger = LogManager.getLogger(PictureScanningService.class);

  private final PictureService pictureService;
  private static final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();
  private final FileUtilitiesService fileUtilitiesService;
  private final RecursiveWatcherService watcherService;
  private final FileTransferWatcher transferWatcher;
  private final PictureFileScanner pictureFileScanner;

  public PictureScanningService(
      PictureService pictureService,
      PictureFileScanner pictureFileScanner,
      FileUtilitiesService fileUtilitiesService,
      RecursiveWatcherService watcherService,
      FileTransferWatcher transferWatcher) {
    this.pictureService = pictureService;
    this.pictureFileScanner = pictureFileScanner;
    this.fileUtilitiesService = fileUtilitiesService;
    this.watcherService = watcherService;
    this.transferWatcher = transferWatcher;
  }

  @Override
  @Async("fileScanner")
  public void scanFile(@NotNull Path file, @NotNull Collection<Path> existing) {
    try {
      fileUtilitiesService.repairFilename(file);
      if (!existing.contains(file)) {
        createPicture(file);
      } else {
        logger.trace("Picture already exists: {}", file);
      }
    } catch (Throwable e) {
      logger.error("Found invalid Picture file: {}", file);
      String ext = FilenameUtils.getExtension(file.toString());
      invalidFiles.add(ext, file);
    }
  }

  private void createPicture(@NotNull Path file) {
    Picture picture =
        Picture.pictureBuilder()
            .uri(file.toUri())
            .title(FilenameUtils.getBaseName(file.toString()))
            .build();
    logger.info("Adding new Picture: {}", picture);
    pictureFileScanner.scanFileMetadata(picture);
  }

  @Override
  public void handleFileEvent(@NotNull Path file, @NotNull WatchEvent.Kind<?> kind) {
    logger.trace("Event: {} happened to picture: {}", kind, file);
    if (ENTRY_CREATE.equals(kind)) {
      if (Files.isDirectory(file)) {
        logger.info("Detected new Picture directory: {}", file);
        watcherService.walkTreeAndSetWatches(
            file, path -> this.scanFile(path, new ArrayList<>()), this::handleFileEvent, null);
      } else {
        logger.info("Found new Picture: {}", file);
        transferWatcher.watchFileTransfer(file, this::createPicture);
      }
    } else if (ENTRY_MODIFY.equals(kind)) {
      transferWatcher.watchFileTransfer(file, this::createPicture);
    } else if (ENTRY_DELETE.equals(kind)) {
      logger.info("Deleting Picture at: {}", file);
      String ext = FilenameUtils.getExtension(file.toString());
      List<Path> invalidPaths = this.getInvalidFiles().get(ext);
      if (invalidPaths != null) {
        boolean removed = invalidPaths.remove(file);
        if (removed) {
          logger.info("Invalid file: {} deleted", file);
          return;
        }
      }
      pictureService
          .getPictureByPath(file)
          .forEach(pic -> pictureService.deletePicture(pic.getId()));
    }
  }

  @Override
  public MultiValueMap<String, Path> getInvalidFiles() {
    return invalidFiles;
  }
}
