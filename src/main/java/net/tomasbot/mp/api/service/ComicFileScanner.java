package net.tomasbot.mp.api.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ComicFileScanner implements FileMetadataScanner<ComicPage> {

  private static final Logger logger = LogManager.getLogger(ComicFileScanner.class);

  private final ComicBookService comicService;
  private final TagService tagService;
  private final InvalidFilesService invalidFilesService;

  @Value("${comics.location}")
  private Path comicsLocation;

  public ComicFileScanner(
      ComicBookService comicService,
      TagService tagService,
      InvalidFilesService invalidFilesService) {
    this.comicService = comicService;
    this.tagService = tagService;
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
      assignComicPage(page);
    } catch (IncorrectResultSizeDataAccessException e) {
      logger.error("Duplicate comic book: {}, {}", page, e.getMessage());
    } catch (Throwable e) {
      logger.error("Could not parse Comic Book page {}: {}", page, e.getMessage(), e);
      invalidFilesService.addInvalidFile(Path.of(page.getUri()), ComicPage.class);
    }
  }

  private synchronized void assignComicPage(@NotNull ComicPage page) {
    Path parent = Path.of(page.getUri()).getParent();
    comicService
        .getComicBookAt(parent)
        .ifPresentOrElse(comic -> addPageToComic(page, comic), () -> createComic(page));
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

  private void createComic(@NotNull Image page) {
    Path pageFile = Path.of(page.getUri());
    Path comicDir = pageFile.getParent();
    String comicName = getComicNames(comicDir).removeLast();
    List<Tag> tags = tagService.getTags(comicsLocation.relativize(comicDir));

    ComicBook comic =
        ComicBook.builder()
            .location(comicDir)
            .title(comicName)
            .build();
    comic.getTags().addAll(tags);

    ComicBook saved = comicService.save(comic);
    saved.addImage(page);
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
}
