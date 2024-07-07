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

  @Value("${comics.location}")
  private Path comicsLocation;

  public ComicFileScanner(ComicBookService comicService, TagService tagService) {
    this.comicService = comicService;
    this.tagService = tagService;
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
      assignComicPage(page);
    } catch (IncorrectResultSizeDataAccessException e) {
      logger.error("Duplicate comic book: {}, {}", page, e.getMessage());
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

    Set<Image> images = new HashSet<>();
    images.add(page);
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
}
