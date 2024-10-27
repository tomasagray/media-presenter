package net.tomasbot.mp.api.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PictureFileScanner implements FileMetadataScanner<Picture> {

  private static final Logger logger = LogManager.getLogger(PictureFileScanner.class);

  private final PictureService pictureService;
  private final TagService tagService;
  private final InvalidFilesService invalidFilesService;

  @Value("${pictures.location}")
  private Path pictureLocation;

  public PictureFileScanner(
      PictureService pictureService,
      TagService tagService,
      InvalidFilesService invalidFilesService) {
    this.pictureService = pictureService;
    this.tagService = tagService;
    this.invalidFilesService = invalidFilesService;
  }

  @Override
  @Async("fileScanner")
  public void scanFileMetadata(@NotNull Picture picture) {
    final URI uri = picture.getUri();
    try {
      setPictureTags(picture);

      BufferedImage image = ImageIO.read(uri.toURL());
      picture.setFilesize(new File(uri).length());
      picture.setWidth(image.getWidth());
      picture.setHeight(image.getHeight());
      pictureService.save(picture);
    } catch (Throwable e) {
      logger.error("Could not process Picture {}: {}", uri, e.getMessage(), e);
      invalidFilesService.addInvalidFile(Path.of(uri), Picture.class);
    }
  }

  private synchronized void setPictureTags(@NotNull Picture picture) {
    // get tags from sub dirs
    Path picturePath = Path.of(picture.getUri());
    Path subDirs = pictureLocation.relativize(picturePath);
    List<Tag> tags = tagService.getTags(subDirs);
    picture.getTags().addAll(tags);
  }
}
