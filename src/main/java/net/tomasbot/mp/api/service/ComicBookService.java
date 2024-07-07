package net.tomasbot.mp.api.service;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.db.ComicBookRepository;
import net.tomasbot.mp.db.ComicPageRepository;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Image;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ComicBookService {

  private final ComicBookRepository comicBookRepo;
  private final ComicPageRepository pageRepository;

  public ComicBookService(ComicBookRepository comicBookRepo, ComicPageRepository pageRepository) {
    this.comicBookRepo = comicBookRepo;
    this.pageRepository = pageRepository;
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

  public ComicBook save(@NotNull ComicBook comicBook) {
    return comicBookRepo.saveAndFlush(comicBook);
  }

  public void delete(@NotNull ComicBook comicBook) {
    comicBookRepo.delete(comicBook);
  }
}
