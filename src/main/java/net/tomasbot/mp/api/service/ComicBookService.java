package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.ComicBookRepository;
import net.tomasbot.mp.db.ComicPageRepository;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ComicBookService {

  private static final Logger logger = LogManager.getLogger(ComicBookService.class);

  private final ComicBookRepository comicBookRepo;
  private final ComicPageRepository pageRepository;
  private final TagManagementService tagService;
  private final PathTagService pathTagService;

  @Value("${comics.location}")
  private Path comicsLocation;

  public ComicBookService(
      ComicBookRepository comicBookRepo,
      ComicPageRepository pageRepository,
      TagManagementService tagService,
      PathTagService pathTagService) {
    this.comicBookRepo = comicBookRepo;
    this.pageRepository = pageRepository;
    this.tagService = tagService;
    this.pathTagService = pathTagService;
  }

  public List<ComicBook> getAllComics() {
    return comicBookRepo.findAll();
  }

  public Page<ComicBook> getAllComics(int page, int size) {
    return comicBookRepo.findAll(PageRequest.of(page, size));
  }

  public Page<ComicBook> getLatestComics(int page, int size) {
    return comicBookRepo.findLatest(PageRequest.of(page, size));
  }

  public List<ComicBook> getRandomComics(int count) {
    return comicBookRepo.findRandomComics(PageRequest.ofSize(count));
  }

  public Optional<ComicBook> getComicBook(UUID bookId) {
    return comicBookRepo.findById(bookId);
  }

  public List<ComicPage> getAllPages() {
    return pageRepository.findAll();
  }

  public Collection<ComicPage> getLoosePages() {
    return pageRepository.findLoosePages();
  }

  public Optional<ComicBook> getComicBookAt(Path path) {
    List<ComicBook> comics = comicBookRepo.findComicBooksIn(path);
    if (comics.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(comics.get(0));
  }

  public List<ComicBook> getComicBooksAt(Path path) {
    return comicBookRepo.findComicBooksIn(path);
  }

  public Optional<ComicBook> getComicBookForPage(@NotNull Image page) {
    return comicBookRepo.findComicBookByImagesContaining(page);
  }

  public Optional<UrlResource> getPageData(@NotNull UUID pageId) {
    return pageRepository.findById(pageId).map(image -> UrlResource.from(image.getUri()));
  }

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
  public synchronized void assignComicPage(@NotNull ComicPage page) {
    Path parent = Path.of(page.getUri()).getParent();
    this.getComicBookAt(parent)
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
    List<Tag> tags = pathTagService.getTagsFrom(comicsLocation.relativize(comicDir));

    ComicBook comic = ComicBook.builder().location(comicDir).title(comicName).build();
    comic.getTags().addAll(tags);

    ComicBook saved = this.save(comic);
    saved.addImage(page);
    this.save(saved);
    logger.info("Created new Comic Book: {}", saved);
  }

  private void addPageToComic(@NotNull Image page, @NotNull ComicBook comic) {
    logger.trace("Adding page: {} to Comic Book: {}", page, comic);
    Optional<Image> imgOpt =
        comic.getImages().stream().filter(img -> img.getUri().equals(page.getUri())).findFirst();
    if (imgOpt.isEmpty()) {
      comic.addImage(page);
      this.save(comic);
    } else {
      logger.trace("Page: {} is already in Comic Book: {}", page, comic);
    }
  }

  public ComicBook save(@NotNull ComicBook comicBook) {
    return comicBookRepo.saveAndFlush(comicBook);
  }

  public void delete(@NotNull ComicBook comicBook) {
    comicBookRepo.delete(comicBook);
  }

  public ComicBook updateComic(@NotNull ComicBook update) {
    logger.info("Updating Comic Book: {}", update);

    final UUID comicId = update.getId();
    return (ComicBook)
        comicBookRepo
            .findById(comicId)
            .map(existing -> tagService.update(existing, update))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Cannot update non-existent Comic Book: " + comicId));
  }
}
