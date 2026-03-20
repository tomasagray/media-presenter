package net.tomasbot.mp.util;

import net.tomasbot.mp.api.service.ThumbnailService;
import net.tomasbot.mp.api.service.user.UserComicService;
import net.tomasbot.mp.api.service.user.UserPictureService;
import net.tomasbot.mp.api.service.user.UserVideoService;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.Video;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
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

  public TrashCollectorService(
          UserVideoService videoService,
          ThumbnailService thumbnailService,
          UserPictureService pictureService,
          UserComicService comicService) {
    this.videoService = videoService;
    this.thumbnailService = thumbnailService;
    this.pictureService = pictureService;
    this.comicService = comicService;
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
      logger.info("Checking video at: {}", videoFile);

      if (videoFile.exists()) logger.info("Found video file at: {}", videoFile);
      else {
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
        logger.info("Checking for thumbnail: {} in database...", file);

        Optional<Image> thumbnailOpt = thumbnailService.getThumbnailAt(file);
        if (thumbnailOpt.isEmpty()) {
          logger.info("Thumbnail at: {} not found in database; deleting...", file);
          boolean deleted = file.toFile().delete();
          if (!deleted) throw new IOException("Could not delete stray thumbnail at: " + file);
          delCount.incrementAndGet();
        } else {
          logger.info("Thumbnail was found");
        }

        return FileVisitResult.CONTINUE;
      }
    });

    logger.info("Finished eliminating stray video thumbnails; {} targets neutralized.", delCount.get());
  }

  @Transactional
  public void cleanPictureTrash() {
    int deleted = 0;
    List<Picture> all = pictureService.getAllPictures();

    for (Picture picture : all) {
      logger.info("Checking Picture: {}", picture);
      File picFile = Path.of(picture.getUri()).toFile();

      if (!picFile.exists()) {
        logger.info("Could not find picture at: {}; deleting...", picFile);
        pictureService.deletePicture(picture.getId());
        deleted++;
      }
    }

    logger.info("Finished cleaning broken Picture links; deleted {} broken entries", deleted);
  }

  @Transactional
  public void cleanComicsTrash() throws IOException {
    int deleted = 0;
    List<ComicBook> comics = comicService.getAllComics();

    for (ComicBook comic : comics) {
      logger.info("Checking Comic Book: {}", comic);

      // check missing pages
      int missing = 0;
      List<Image> pages = new ArrayList<>(comic.getImages());

      for (int i = 0; i < pages.size(); i++) {
        Image page = pages.get(i);
        Path path = Path.of(page.getUri());
        if (!path.toFile().exists()) {
          logger.info("Image not found at: {}; deleting page #{} from Comic...", i + 1, page);
          pictureService.deletePicture(page.getId());
          missing++;
        }
      }

      // check CB in DB -> nothing
      File location = comic.getLocation().toFile();
      if (!location.exists() || pages.size() - missing <= 0) {
        logger.info("Comic: {} is empty; deleting...", comic);
        comicService.deleteComic(comic.getId());
        deleted++;
      }

      logger.info("Finished cleaning up Comic Books; deleted {} broken entries", deleted);
    }
  }
}
