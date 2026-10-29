package net.tomasbot.mp.api.service.user;

import lombok.extern.slf4j.Slf4j;
import net.tomasbot.mp.api.service.ComicBookService;
import net.tomasbot.mp.api.service.RandomComicService;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Favorite;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserComicBookView.UserComicModeller;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class UserComicService {

  private static final int MAX_PAGE_SIZE = 25;

  private final UserPreferenceService userPreferenceService;
  private final ComicBookService comicService;
  private final RandomComicService randomComicService;
  private final UserComicModeller modeller;

  public UserComicService(
          UserPreferenceService userPreferenceService,
          ComicBookService comicService,
          RandomComicService randomComicService,
          UserComicModeller modeller) {
    this.userPreferenceService = userPreferenceService;
    this.comicService = comicService;
    this.randomComicService = randomComicService;
    this.modeller = modeller;
  }

  private static int getLimitedCount(int count) {
    return Math.min(count, MAX_PAGE_SIZE);
  }

  public Page<UserComicBookView> getAllUserComics(int page, int size) {
    return comicService.getAllComics(page, size).map(this::getUserComicBookView);
  }

  public Page<UserComicBookView> getLatestUserComics(int page, int size) {
    return comicService.getLatestComics(page, size).map(this::getUserComicBookView);
  }

  @NotNull
  public UserComicBookView getUserComicBookView(@NotNull ComicBook comic) {
    return userPreferenceService.isFavorite(comic)
        ? modeller.toFavorite(comic)
        : modeller.toView(comic);
  }

  public List<UserComicBookView> getRandomUserComics(int count) {
    int limitedCount = getLimitedCount(count);

    List<ComicBook> randomCollection = randomComicService.getRandomCollection();
    if (randomCollection.isEmpty())
      randomCollection = comicService.getRandomComics(limitedCount);

    return randomCollection.stream().limit(limitedCount).map(this::getUserComicBookView).toList();
  }

  public Optional<UserComicBookView> getUserComicBook(UUID comicId) {
    return comicService.getComicBook(comicId).map(this::getUserComicBookView);
  }

  public UserComicBookView toggleIsComicFavorite(@NotNull UUID comicId) {
    Optional<ComicBook> optional = comicService.getComicBook(comicId);
    if (optional.isEmpty())
      throw new IllegalArgumentException("Trying to favorite non-existent Comic Book: " + comicId);

    ComicBook comic = optional.get();
    if (!userPreferenceService.isFavorite(comic)) {
      if (userPreferenceService.setFavorite(comic)) return modeller.toFavorite(comic);
      else throw new SetFavoriteException(comic);
    } else {
      if (userPreferenceService.removeFavorite(comic)) return modeller.toView(comic);
      else throw new RemoveFavoriteException(comic);
    }
  }

  public Page<UserComicBookView> getFavoriteComics(int page, int size) {
    int limitedSize = getLimitedCount(size);

    return userPreferenceService.getUserComicFavorites(page, limitedSize)
            .map(Favorite::getEntityId)
            .map(comicService::getComicBook)
            .map(Optional::get)
            .map(this::getUserComicBookView);
  }

  public void unfavoriteForAllUsers(ComicBook comic) {
    userPreferenceService.getCurrentUserPreferences().getFavoriteComics().stream()
            .filter(favorite -> favorite.getEntityId().equals(comic.getId()))
            .forEach(favorite -> userPreferenceService.removeFavorite(favorite.getEntityId()));
  }

  // === Passthru methods ===
  public List<ComicBook> getAllComics() {
    return comicService.getAllComics();
  }

  public Optional<UrlResource> getPageData(UUID pageId) {
    return comicService.getPageData(pageId);
  }

  public UserComicBookView updateComic(@NotNull UserComicBookView comicView) {
    ComicBook comicBook = modeller.fromView(comicView);
    ComicBook updated = comicService.updateComic(comicBook);
    return modeller.toView(updated);
  }

  public void recycleComic(@NotNull UUID comicId) throws IOException {
    Optional<ComicBook> comicOpt = comicService.getComicBook(comicId);
    if (comicOpt.isPresent()) {
      ComicBook comic = comicOpt.get();
      this.unfavoriteForAllUsers(comic);
      randomComicService.deleteContaining(comicId);
      comicService.recycleComic(comic);
    }
  }

  public void deleteComic(@NotNull UUID comicId) {
    Optional<ComicBook> comicOpt = comicService.getComicBook(comicId);
    if (comicOpt.isPresent()) {
      ComicBook comic = comicOpt.get();
      this.unfavoriteForAllUsers(comic);
      randomComicService.deleteContaining(comicId);
      comicService.delete(comic);
    }
  }

  public void deletePage(@NotNull ComicPage page) {
    comicService.deletePage(page);
  }
}
