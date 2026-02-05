package net.tomasbot.mp.api.service.user;

import lombok.extern.slf4j.Slf4j;
import net.tomasbot.mp.api.service.ComicBookService;
import net.tomasbot.mp.api.service.RandomComicService;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.Favorite;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserComicBookView.UserComicModeller;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional
public class UserComicService {

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

  public Collection<UserComicBookView> getUserComicViews(@NotNull Collection<ComicBook> comics) {
    return comics.stream().map(this::getUserComicBookView).toList();
  }

  public List<UserComicBookView> getRandomUserComics() {
    List<ComicBook> randomCollection = randomComicService.getRandomCollection();
    if (randomCollection.isEmpty())
      randomCollection = comicService.getRandomComics(18);
    return randomCollection.stream().map(this::getUserComicBookView).toList();
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

  public Collection<UserComicBookView> getFavoriteComics() {
    return userPreferenceService.getCurrentUserPreferences().getFavoriteComics().stream()
        .sorted(Comparator.comparing(Favorite::getTimestamp).reversed())
        .map(Favorite::getEntityId)
        .map(comicService::getComicBook)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::getUserComicBookView)
        .toList();
  }

  public Optional<UrlResource> getPageData(UUID pageId) {
    return comicService.getPageData(pageId);
  }

  public UserComicBookView updateComic(@NotNull UserComicBookView comicView) {
    ComicBook comicBook = modeller.fromView(comicView);
    ComicBook updated = comicService.updateComic(comicBook);
    return modeller.toView(updated);
  }
}
