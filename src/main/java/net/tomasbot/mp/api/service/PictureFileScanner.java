package net.tomasbot.mp.api.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PictureFileScanner implements FileMetadataScanner<Picture> {
  
  private final PictureService pictureService;
  private final PathTagService pathTagService;

  @Value("${pictures.location}")
  private Path pictureLocation;

  public PictureFileScanner(PictureService pictureService, PathTagService pathTagService) {
    this.pictureService = pictureService;
    this.pathTagService = pathTagService;
  }

  @Override
  @Async("fileScanner")
  public void scanFileMetadata(@NotNull Picture picture) throws IOException {
    final URI uri = picture.getUri();
    setPictureTags(picture);

    BufferedImage image = ImageIO.read(uri.toURL());
    picture.setFilesize(new File(uri).length());
    picture.setWidth(image.getWidth());
    picture.setHeight(image.getHeight());
    pictureService.save(picture);
  }

  private synchronized void setPictureTags(@NotNull Picture picture) {
    // get tags from sub dirs
    Path picturePath = Path.of(picture.getUri());
    Path subDirs = pictureLocation.relativize(picturePath);
    List<Tag> tags = pathTagService.getTagsFrom(subDirs);
    picture.getTags().addAll(tags);
  }
}
