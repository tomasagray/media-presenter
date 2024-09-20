package net.tomasbot.mp.api.service.user;

import java.util.*;
import net.tomasbot.mp.db.UserPreferencesRepository;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.UserPreferences;
import net.tomasbot.mp.model.Video;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserPreferenceService {

  private final Logger logger = LogManager.getLogger(UserPreferenceService.class);

  private final UserPreferencesRepository repository;

  public UserPreferenceService(UserPreferencesRepository repository) {
    this.repository = repository;
  }

  public UserDetails createUserPreferences(@NotNull UserDetails user) {
    logger.info("Creating new User Preferences for: {}", user);
    UserPreferences preferences = new UserPreferences(user.getUsername());
    repository.save(preferences);
    return user;
  }

  public List<UserPreferences> getAllUserPreferences() {
    return repository.findAll();
  }

  public UserPreferences getCurrentUserPreferences() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    UserDetails user = (UserDetails) auth.getPrincipal();
    return getUserPreferences(user.getUsername());
  }

  public UserPreferences getUserPreferences(@NotNull String username) {
    logger.trace("Getting User Preferences for: {}", username);
    Optional<UserPreferences> optional = repository.findByUsername(username);
    if (optional.isEmpty()) {
      throw new IllegalArgumentException("Unknown user: " + username);
    }
    return optional.get();
  }

  public void deleteUserPreferences(@NotNull UUID prefId) {
    repository.deleteById(prefId);
  }

  private static <T> boolean toggleFavorite(@NotNull Collection<T> favorites, T entity) {
    if (favorites.contains(entity)) {
      favorites.remove(entity);
      return false;
    } else {
      favorites.add(entity);
      return true;
    }
  }

  public boolean isFavorite(Video video) {
    return getCurrentUserPreferences().getFavoriteVideos().stream()
        .map(Video::getId)
        .anyMatch(id -> id.equals(video.getId()));
  }

  public boolean isFavorite(Picture picture) {
    return getCurrentUserPreferences().getFavoritePictures().stream()
        .map(Picture::getId)
        .anyMatch(id -> id.equals(picture.getId()));
  }

  public boolean isFavorite(ComicBook comic) {
    return getCurrentUserPreferences().getFavoriteComics().stream()
        .map(ComicBook::getId)
        .anyMatch(id -> id.equals(comic.getId()));
  }

  public boolean setFavorite(Object o) {
    return setFavorite(getCurrentUserPreferences(), o);
  }

  public boolean setFavorite(@NotNull UserPreferences preferences, Object o) {
    logger.info("Setting {} as favorite for user: {}", o, preferences.getUsername());
    if (o instanceof Video) {
      Set<Video> favoriteVideos = preferences.getFavoriteVideos();
      if (!favoriteVideos.contains((Video) o)) return favoriteVideos.add((Video) o);
      else return false;
    } else if (o instanceof Picture) {
      Set<Picture> favoritePictures = preferences.getFavoritePictures();
      if (!favoritePictures.contains(o)) return favoritePictures.add((Picture) o);
      else return false;
    } else if (o instanceof ComicBook) {
      Set<ComicBook> favoriteComics = preferences.getFavoriteComics();
      if (!favoriteComics.contains(o)) return favoriteComics.add((ComicBook) o);
      else return false;
    } else
      throw new IllegalArgumentException("Cannot favorite unknown type: " + o.getClass().getName());
  }

  public boolean removeFavorite(Object o) {
    return removeFavorite(getCurrentUserPreferences(), o);
  }

  public boolean removeFavorite(@NotNull UserPreferences preferences, Object o) {
    logger.info("Removing user [{}] favorite from {}", preferences.getUsername(), o);
    if (o instanceof Video) {
      return preferences.getFavoriteVideos().remove((Video) o);
    } else if (o instanceof Picture) {
      return preferences.getFavoritePictures().remove((Picture) o);
    } else if (o instanceof ComicBook) {
      return preferences.getFavoriteComics().remove((ComicBook) o);
    } else
      throw new IllegalArgumentException(
          "Cannot unfavorite unknown type: " + o.getClass().getName());
  }

  public boolean toggleFavorite(Video video) {
    Set<Video> favoriteVideos = getCurrentUserPreferences().getFavoriteVideos();
    return toggleFavorite(favoriteVideos, video);
  }

  public boolean toggleFavorite(Picture picture) {
    Set<Picture> favoritePictures = getCurrentUserPreferences().getFavoritePictures();
    return toggleFavorite(favoritePictures, picture);
  }

  public boolean toggleFavorite(ComicBook comicBook) {
    Set<ComicBook> favoriteComics = getCurrentUserPreferences().getFavoriteComics();
    return toggleFavorite(favoriteComics, comicBook);
  }
}
