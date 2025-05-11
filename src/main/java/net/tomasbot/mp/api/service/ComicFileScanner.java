package net.tomasbot.mp.api.service;

import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@Component
public class ComicFileScanner implements FileMetadataScanner<ComicPage> {

  private static final Logger logger = LogManager.getLogger(ComicFileScanner.class);

  private final ComicBookService comicService;
  private final InvalidFilesService invalidFilesService;

  public ComicFileScanner(ComicBookService comicService, InvalidFilesService invalidFilesService) {
    this.comicService = comicService;
    this.invalidFilesService = invalidFilesService;
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
  public void scanFileMetadata(@NotNull ComicPage page) {
    try {
      if (imageRequiresParsing(page)) parseImage(page);
      comicService.assignComicPage(page);
    } catch (IncorrectResultSizeDataAccessException e) {
      logger.error("Duplicate comic book: {}, {}", page, e.getMessage());
    } catch (Throwable e) {
      logger.error("Could not scan ComicPage metadata: {}", e.getMessage());
      logger.debug(e);
      invalidFilesService.addInvalidFile(Path.of(page.getUri()), ComicPage.class);
    }
  }
}
