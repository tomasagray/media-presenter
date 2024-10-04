package net.tomasbot.mp.api.service.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.api.service.ComicBookService;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserComicBookView.UserComicModeller;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserComicService {

  private final UserPreferenceService userPreferenceService;
  private final ComicBookService comicService;
  private final UserComicModeller modeller;

  public UserComicService(
          UserPreferenceService userPreferenceService, ComicBookService comicService, UserComicModeller modeller) {
    this.userPreferenceService = userPreferenceService;
    this.comicService = comicService;
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

  public List<UserComicBookView> getRandomUserComics(int count) {
    return comicService.getRandomComics(count).stream().map(this::getUserComicBookView).toList();
  }

  public Optional<UserComicBookView> getUserComicBook(UUID comicId) {
    return comicService.getComicBook(comicId).map(this::getUserComicBookView);
  }

  public UserComicBookView toggleIsComicFavorite(@NotNull UUID comicId) {
    Optional<ComicBook> optional = comicService.getComicBook(comicId);
    if (optional.isEmpty()) {
      throw new IllegalArgumentException("Trying to favorite non-existent Comic Book: " + comicId);
    }
    ComicBook comic = optional.get();
    if (userPreferenceService.toggleFavorite(comic)) {
      return modeller.toFavorite(comic);
    }
    return modeller.toView(comic);
  }

  public Collection<UserComicBookView> getFavoriteComics() {
    return userPreferenceService.getCurrentUserPreferences().getFavoriteComics().stream()
        .map(comicService::getComicBook)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::getUserComicBookView)
        .toList();
  }

  public Optional<UrlResource> getPageData(UUID pageId) {
    return comicService.getPageData(pageId);
  }
}
