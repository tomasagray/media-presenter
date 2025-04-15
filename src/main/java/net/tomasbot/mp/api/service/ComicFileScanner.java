package net.tomasbot.mp.api.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import javax.imageio.ImageIO;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ComicFileScanner implements FileMetadataScanner<ComicPage> {

  private static final Logger logger = LogManager.getLogger(ComicFileScanner.class);

  private final ComicBookService comicService;

  public ComicFileScanner(ComicBookService comicService) {
    this.comicService = comicService;
  }

  private boolean imageRequiresParsing(@NotNull Image image) {
    return image.getHeight() == 0 || image.getWidth() == 0 || image.getFilesize() == 0;
  }

  private void parseImage(@NotNull Image image) throws IOException {
    final URI uri = image.getUri();
    BufferedImage data = ImageIO.read(uri.toURL());
    image.setHeight(data.getHeight());
    image.setWidth(data.getWidth());
    image.setFilesize(new File(uri).length());
  }

  @Override
  @Async("fileScanner")
  public void scanFileMetadata(@NotNull ComicPage page) throws IOException {
    try {
      if (imageRequiresParsing(page)) parseImage(page);
      comicService.assignComicPage(page);
    } catch (IncorrectResultSizeDataAccessException e) {
      logger.error("Duplicate comic book: {}, {}", page, e.getMessage());
    }
  }
}
