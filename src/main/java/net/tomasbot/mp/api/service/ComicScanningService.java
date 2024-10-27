package net.tomasbot.mp.api.service;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import net.tomasbot.mp.db.ImageRepository;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Image;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ComicScanningService implements FileScanningService {

  private static final Logger logger = LogManager.getLogger(ComicScanningService.class);

  private final ImageRepository imageRepository;
  private final ComicBookService comicService;
  private final ComicFileScanner comicFileScanner;
  private final RecursiveWatcherService watcherService;
  private final FileTransferWatcher transferWatcher;
  private final FileUtilitiesService fileUtilitiesService;
  private final InvalidFilesService invalidFilesService;

  @Value("${comics.location}")
  private Path comicsLocation;

  public ComicScanningService(
      ImageRepository imageRepository,
      ComicBookService comicService,
      ComicFileScanner comicFileScanner,
      RecursiveWatcherService watcherService,
      FileTransferWatcher transferWatcher,
      FileUtilitiesService fileUtilitiesService,
      InvalidFilesService invalidFilesService) {
    this.imageRepository = imageRepository;
    this.comicService = comicService;
    this.comicFileScanner = comicFileScanner;
    this.watcherService = watcherService;
    this.transferWatcher = transferWatcher;
    this.fileUtilitiesService = fileUtilitiesService;
    this.invalidFilesService = invalidFilesService;
  }

  @Override
  public void scanFile(@NotNull Path file, @NotNull Collection<Path> existing) {
    try {
      fileUtilitiesService.repairFilename(file);
      logger.trace("Found Comic Book page: {}", file);
      Path parent = file.getParent();
      if (!isPathWithin(comicsLocation, parent)) {
        throw new IOException("Path is not within a Comic Book: " + file);
      }
      if (!existing.contains(file)) {
        createComicPage(file);
      } else {
        logger.trace("Comic Book page already in database: {}", file);
      }
    } catch (Throwable e) {
      logger.error("File could not be added to Comic: {}; {}", file, e.getMessage(), e);
      invalidFilesService.addInvalidFile(file, ComicPage.class);
    }
  }

  private void createComicPage(@NotNull Path file) {
    try {
      ComicPage page =
          ComicPage.pageBuilder()
              .uri(file.toUri())
              .title(FilenameUtils.getBaseName(file.toString()))
              .build();
      comicFileScanner.scanFileMetadata(page);
    } catch (Throwable e) {
      logger.error("Could not scan Comic Book page: {}: {}", file, e.getMessage(), e);
      invalidFilesService.addInvalidFile(file, ComicPage.class);
    }
  }

  private boolean isPathWithin(@NotNull Path parent, @NotNull Path child) {
    Path absParent = parent.toAbsolutePath();
    Path absChild = child.toAbsolutePath();
    return absChild.startsWith(absParent) && !absChild.equals(absParent);
  }
  
  @Override
  public void handleFileEvent(@NotNull Path file, WatchEvent.@NotNull Kind<?> kind) {
    if (ENTRY_CREATE.equals(kind)) {
      if (Files.isDirectory(file)) {
        logger.info("Detected new Comic Book: {}", file);
        watcherService.walkTreeAndSetWatches(
            file, page -> this.scanFile(page, new ArrayList<>()), this::handleFileEvent, null);
      } else {
        logger.info("Found new Comic Book page: {}", file);
        transferWatcher.watchFileTransfer(file, this::createComicPage);
      }
    } else if (ENTRY_MODIFY.equals(kind)) {
      transferWatcher.watchFileTransfer(file, this::createComicPage);
    } else if (ENTRY_DELETE.equals(kind)) {
      String prefix =
          invalidFilesService.deleteInvalidFile(file, ComicPage.class) ? "Invalid " : "";
      logger.info("{}Comic Book image was deleted: {}", prefix, file);
      handleDeletedImage(file);
    }
  }

  private void handleDeletedImage(Path file) {
    List<ComicBook> comics = comicService.getComicBooksAt(file);
    if (!comics.isEmpty()) {
      comics.forEach(comicService::delete);
      return;
    }

    List<Image> images = imageRepository.findByUri(file.toUri());
    if (!images.isEmpty()) {
      for (Image image : images) {
        deleteImage(image);
      }
    } else {
      logger.warn("Detected deletion of unknown Comic Book Image: {}", file);
    }
  }

  private void deleteImage(@NotNull Image image) {
    invalidFilesService.deleteInvalidFile(Path.of(image.getUri()), ComicPage.class);

    Optional<ComicBook> comicOpt = comicService.getComicBookForPage(image);
    if (comicOpt.isPresent()) {
      ComicBook comicBook = comicOpt.get();
      boolean removed = comicBook.getImages().remove(image);
      if (!removed) {
        throw new IllegalStateException("Could not remove Image from Comic Book: " + image);
      }
      if (comicBook.getImages().isEmpty()) {
        comicService.delete(comicBook);
      } else {
        comicService.save(comicBook);
      }
    } else {
      logger.warn("Image deleted was not part of a Comic Book: {}", image);
    }
  }
}
