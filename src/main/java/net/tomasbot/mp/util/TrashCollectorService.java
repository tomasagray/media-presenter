package net.tomasbot.mp.util;

import net.tomasbot.mp.api.service.ThumbnailService;
import net.tomasbot.mp.api.service.user.UserComicService;
import net.tomasbot.mp.api.service.user.UserPictureService;
import net.tomasbot.mp.api.service.user.UserVideoService;
import net.tomasbot.mp.db.ComicPageRepository;
import net.tomasbot.mp.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TrashCollectorService {

  private static final Logger logger = LogManager.getLogger(TrashCollectorService.class);

  private final UserVideoService videoService;
  private final ThumbnailService thumbnailService;
  private final UserPictureService pictureService;
  private final UserComicService comicService;
  private final ComicPageRepository comicPageRepo;

  public TrashCollectorService(
          UserVideoService videoService,
          ThumbnailService thumbnailService,
          UserPictureService pictureService,
          UserComicService comicService,
          ComicPageRepository comicPageRepo) {
    this.videoService = videoService;
    this.thumbnailService = thumbnailService;
    this.pictureService = pictureService;
    this.comicService = comicService;
    this.comicPageRepo = comicPageRepo;
  }

  /**
   * Scans the video repository for entries which do not correspond to an existing
   * location on the filesystem and deletes them.
   */
  @Transactional
  public void cleanVideoTrash() throws IOException {
    int deleted = 0;
    logger.info("Beginning purge of broken video entries in database...");

    List<Video> videos = videoService.getAllVideos();
    for (Video video : videos) {
      File videoFile = video.getLocation().toFile();
      logger.trace("Checking video at: {}", videoFile);

      if (!videoFile.exists()) {
        logger.warn("Could not find video at: {}; deleting database entry...", videoFile);

        videoService.deleteVideo(video.getId());
        deleted++;
      }
    }

    logger.info("Finished cleaning video trash; deleted {} broken entries.", deleted);
  }

  public void deleteStrayThumbnails() throws IOException {
    final AtomicInteger delCount = new AtomicInteger(0);
    Path thumbLocation = thumbnailService.getThumbLocation();

    Files.walkFileTree(thumbLocation, new SimpleFileVisitor<>() {
      @Override
      public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
        logger.trace("Checking for thumbnail: {} in database...", file);

        Optional<Image> thumbnailOpt = thumbnailService.getThumbnailAt(file);
        if (thumbnailOpt.isEmpty()) {
          logger.warn("Thumbnail at: {} not found in database; deleting...", file);
          boolean deleted = file.toFile().delete();
          if (!deleted) throw new IOException("Could not delete stray thumbnail at: " + file);
          delCount.incrementAndGet();
        }

        return FileVisitResult.CONTINUE;
      }
    });

    logger.info("Finished eliminating stray video thumbnails; {} targets neutralized.", delCount.get());
  }

  @Transactional
  public void deleteBrokenThumbnails() {
    logger.info("Beginning purge of broken thumbnails...");
    videoService.getAllVideos()
            .stream().map(Video::getThumbnails)
            .forEach(this::fixBrokenThumbs);
    logger.info("Finished cleaning broken thumbnails.");
  }

  private void fixBrokenThumbs(@NonNull ImageSet thumbs) {
    List<Image> broken = new ArrayList<>();

    thumbs.getImages().forEach(thumb -> {
      File file = Path.of(thumb.getUri()).toFile();
      if (!file.exists()) broken.add(thumb);
    });

    broken.forEach(thumb -> deleteThumb(thumbs, thumb));
  }

  private void deleteThumb(@NotNull ImageSet thumbs, @NotNull Image thumb) {
    URI uri = thumb.getUri();
    try {
      logger.warn("Thumbnail at: {} does not exist; deleting...", uri);
      thumbnailService.deleteThumbnail(thumb);
      thumbs.removeImage(thumb);
    } catch (IOException e) {
      logger.error("Could not delete broken thumbnail at: {}", uri, e);
    }
  }

  @Transactional
  public void cleanPictureTrash() {
    int deleted = 0;
    List<Picture> all = pictureService.getAllPictures();

    for (Picture picture : all) {
      logger.trace("Checking Picture: {}", picture);
      File picFile = Path.of(picture.getUri()).toFile();

      if (!picFile.exists()) {
        logger.warn("Could not find picture at: {}; deleting...", picFile);
        pictureService.deletePicture(picture.getId());
        deleted++;
      }
    }

    logger.info("Finished cleaning broken Picture links; deleted {} broken entries", deleted);
  }

  @Transactional
  public void cleanComicsTrash() {
    // clean pages first
    cleanBrokenComicPages();

    int deleted = 0, missingPages = 0;
    List<ComicBook> comics = comicService.getAllComics();

    for (ComicBook comic : comics) {
      logger.trace("Checking Comic Book: {}", comic);

      // check missing pages
      int missing = checkForMissingPages(comic);
      if (missing > 0) {
        missingPages += missing;
        logger.warn("Comic {} had {} missing pages", comic.getId(), missing);
      }

      // check for empty Comics, broken references
      File location = comic.getLocation().toFile();
      if (!location.exists() || comic.isEmpty()) {
        logger.warn("Comic: {} is empty; deleting...", comic);
        comicService.deleteComic(comic.getId());
        deleted++;
      }
    }

    logger.info("Finished cleaning up Comic Books; cleaned up {} missing pages, " +
            "deleted {} broken entries", missingPages, deleted);
  }

  private int checkForMissingPages(@NonNull ComicBook comic) {
    int missing = 0;
    List<Image> pages = new ArrayList<>(comic.getImages());

    for (int i = 0; i < pages.size(); i++) {
      ComicPage page = (ComicPage) pages.get(i);
      logger.trace("Checking page {}", page);
      Path path = Path.of(page.getUri());

      if (!path.toFile().exists()) {
        logger.warn("Image not found at: {}; deleting page #{} from Comic...", path, i + 1);
        comic.removeImage(page);
        comicService.deletePage(page);
        missing++;
      }
    }

    return missing;
  }

  private void cleanBrokenComicPages() {
    int deleted = 0;
    List<ComicPage> allPages = comicPageRepo.findAll();

    for (ComicPage page : allPages) {
      logger.trace("Checking page: {}", page);

      File pageFile = Path.of(page.getUri()).toFile();
      if (!pageFile.exists()) {
        logger.warn("Page file not found at: {}; deleting page...", pageFile);
        comicPageRepo.delete(page);
        deleted++;
      }
    }

    logger.info("Finished cleaning up Comic Book Pages; deleted {} broken entries", deleted);
  }
}
