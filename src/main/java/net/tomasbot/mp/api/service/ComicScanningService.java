package net.tomasbot.mp.api.service;

import static java.nio.file.StandardWatchEventKinds.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import javax.imageio.ImageIO;
import net.tomasbot.mp.db.ImageRepository;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Tag;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class ComicScanningService implements FileScanningService {

  private static final Logger logger = LogManager.getLogger(ComicScanningService.class);
  private static final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();
  private final ImageRepository imageRepository;
  private final ComicBookService comicService;
  private final TagService tagService;
  private final RecursiveWatcherService watcherService;
  private final FileTransferWatcher transferWatcher;
  private final FileUtilitiesService fileUtilitiesService;

  private final Set<Image> scannedImages = new HashSet<>();

  @Value("${comics.location}")
  private Path comicsLocation;

  public ComicScanningService(
      ImageRepository imageRepository,
      ComicBookService comicService,
      TagService tagService,
      RecursiveWatcherService watcherService,
      FileTransferWatcher transferWatcher,
      FileUtilitiesService fileUtilitiesService) {
    this.imageRepository = imageRepository;
    this.comicService = comicService;
    this.tagService = tagService;
    this.watcherService = watcherService;
    this.transferWatcher = transferWatcher;
    this.fileUtilitiesService = fileUtilitiesService;
  }

  @Override
  public void scanFile(@NotNull Path file, @NotNull Collection<Path> existing) {
    try {
      fileUtilitiesService.repairFilename(file);
      logger.trace("Found Comic Book page: {}", file);
      Path parent = file.getParent();
      if (!isPathWithin(comicsLocation, parent)) {
        throw new UncheckedIOException(new IOException("Path is not within a Comic Book: " + file));
      }
      if (!existing.contains(file)) {
        Image page =
            ComicPage.pageBuilder()
                .uri(file.toUri())
                .title(FilenameUtils.getBaseName(file.toString()))
                .build();
        scannedImages.add(page);
      } else {
        logger.trace("Comic Book page already in database: {}", file);
      }
    } catch (Throwable e) {
      logger.error("File could not be added to Comic: {}; {}", file, e.getMessage(), e);
      String ext = FilenameUtils.getExtension(file.toString());
      invalidFiles.add(ext, file);
    }
  }

  @Override
  public synchronized void saveScannedData() {
    List<Image> savable = scannedImages.stream().filter(Objects::nonNull).toList();
    logger.info("Saving all {} new Comic Book pages to database....", savable.size());
    imageRepository.saveAll(savable);
    scannedImages.clear();
  }

  private boolean imageRequiresParsing(@NotNull Image image) {
    return image.getHeight() == 0 || image.getWidth() == 0 || image.getFilesize() == 0;
  }

  private void parseImage(@NotNull Image image) {
    final URI uri = image.getUri();
    try {
      BufferedImage data = ImageIO.read(uri.toURL());
      image.setHeight(data.getHeight());
      image.setWidth(data.getWidth());
      image.setFilesize(new File(uri).length());
    } catch (Throwable e) {
      Path path = Path.of(uri);
      logger.error("Could not read image data for {}: {}", path, e.getMessage());
      String ext = FilenameUtils.getExtension(path.toString());
      invalidFiles.add(ext, path);
    }
  }

  @Async("fileScanner")
  @Transactional
  public synchronized void addPageOrCreateComic(@NotNull Image page) {
    try {
      if (imageRequiresParsing(page)) {
        parseImage(page);
      }
      Path parent = Path.of(page.getUri()).getParent();
      comicService
          .getComicBookAt(parent)
          .ifPresentOrElse(comic -> addPageToComic(page, comic), () -> createComic(page));
    } catch (IncorrectResultSizeDataAccessException e) {
      logger.error("Duplicate comic book: {}, {}", page, e.getMessage());
    }
  }

  private boolean isPathWithin(@NotNull Path parent, @NotNull Path child) {
    Path absParent = parent.toAbsolutePath();
    Path absChild = child.toAbsolutePath();
    return absChild.startsWith(absParent) && !absChild.equals(absParent);
  }

  private synchronized void createComic(@NotNull Image page) {
    Set<Image> images = new HashSet<>();
    images.add(page);

    Path pageFile = Path.of(page.getUri());
    Path parent = pageFile.getParent();
    LinkedList<String> names = getComicNames(parent);
    String comicName = names.removeLast();
    List<Tag> tags = tagService.getTags(comicsLocation.relativize(pageFile));

    ComicBook comic =
        ComicBook.builder()
            .location(parent)
            .title(comicName)
            .tags(new HashSet<>(tags)) // ensure mutable
            .build();
    ComicBook saved = comicService.save(comic);
    saved.setImages(images);
    comicService.save(saved);
    logger.info("Created new Comic Book: {}", saved);
  }

  private void addPageToComic(@NotNull Image page, @NotNull ComicBook comic) {
    logger.trace("Adding page: {} to Comic Book: {}", page, comic);
    Optional<Image> imgOpt =
        comic.getImages().stream().filter(img -> img.getUri().equals(page.getUri())).findFirst();
    if (imgOpt.isEmpty()) {
      comic.addImage(page);
      comicService.save(comic);
    } else {
      logger.trace("Page: {} is already in Comic Book: {}", page, comic);
    }
  }

  @NotNull
  private LinkedList<String> getComicNames(Path parent) {
    Path relativized = comicsLocation.relativize(parent);
    LinkedList<String> names = new LinkedList<>();
    for (int i = 0; i < relativized.getNameCount(); i++) {
      names.add(relativized.getName(i).toString());
    }
    return names;
  }

  @Override
  public MultiValueMap<String, Path> getInvalidFiles() {
    return invalidFiles;
  }

  @Override
  public synchronized void handleFileEvent(@NotNull Path file, WatchEvent.@NotNull Kind<?> kind) {
    if (ENTRY_CREATE.equals(kind)) {
      if (Files.isDirectory(file)) {
        logger.info("Detected new Comic Book: {}", file);
        watcherService.walkTreeAndSetWatches(
            file,
            page -> this.scanFile(page, new ArrayList<>()),
            this::handleFileEvent,
            this::processScannedImages);
      } else {
        logger.info("Found new Comic Book page: {}", file);
        transferWatcher.watchFileTransfer(file, this::finishScanComicPage);
      }
    } else if (ENTRY_MODIFY.equals(kind)) {
      transferWatcher.watchFileTransfer(file, this::finishScanComicPage);
    } else if (ENTRY_DELETE.equals(kind)) {
      logger.info("Comic Book image was deleted: {}", file);
      String ext = FilenameUtils.getExtension(file.toString());
      List<Path> invalidPaths = this.getInvalidFiles().get(ext);
      if (invalidPaths != null) {
        boolean removed = invalidPaths.remove(file);
        if (removed) {
          logger.info("Deleted invalid file: {}", file);
          return;
        }
      }
      handleDeletedImage(file);
    }
  }

  private void finishScanComicPage(Path doneFile) {
    scanFile(doneFile, new ArrayList<>());
    processScannedImages();
  }

  @SuppressWarnings("SpringTransactionalMethodCallsInspection")
  private void processScannedImages() {
    List<Image> images = new ArrayList<>(scannedImages);
    saveScannedData();
    for (Image image : images) {
      addPageOrCreateComic(image);
    }
  }

  private void handleDeletedImage(Path file) {
    Optional<ComicBook> comicBookOptional = comicService.getComicBookAt(file);
    if (comicBookOptional.isPresent()) {
      comicService.delete(comicBookOptional.get());
      return;
    }

    Optional<Image> imgOpt = imageRepository.findByUri(file.toUri());
    if (imgOpt.isPresent()) {
      Image image = imgOpt.get();
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
    } else {
      logger.warn("Detected deletion of unknown Comic Book Image: {}", file);
    }
  }
}
