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

  private static boolean toggleFavorite(@NotNull Collection<UUID> favorites, UUID id) {
    if (favorites.contains(id)) {
      favorites.remove(id);
      return false;
    } else {
      favorites.add(id);
      return true;
    }
  }

  public boolean isFavorite(Video video) {
    return getCurrentUserPreferences().getFavoriteVideos().stream()
        .anyMatch(id -> id.equals(video.getId()));
  }

  public boolean isFavorite(Picture picture) {
    return getCurrentUserPreferences().getFavoritePictures().stream()
        .anyMatch(id -> id.equals(picture.getId()));
  }

  public boolean isFavorite(ComicBook comic) {
    return getCurrentUserPreferences().getFavoriteComics().stream()
        .anyMatch(id -> id.equals(comic.getId()));
  }

  public boolean setFavorite(Object o) {
    return setFavorite(getCurrentUserPreferences(), o);
  }

  public boolean setFavorite(@NotNull UserPreferences preferences, Object o) {
    logger.info("Setting {} as favorite for user: {}", o, preferences.getUsername());
    if (o instanceof Video) {
      Set<UUID> favoriteVideos = preferences.getFavoriteVideos();
      UUID videoId = ((Video) o).getId();
      if (!favoriteVideos.contains(videoId)) return favoriteVideos.add(videoId);
      else return false;
    } else if (o instanceof Picture) {
      Set<UUID> favoritePictures = preferences.getFavoritePictures();
      UUID pictureId = ((Picture) o).getId();
      if (!favoritePictures.contains(pictureId)) return favoritePictures.add(pictureId);
      else return false;
    } else if (o instanceof ComicBook) {
      Set<UUID> favoriteComics = preferences.getFavoriteComics();
      UUID comicId = ((ComicBook) o).getId();
      if (!favoriteComics.contains(comicId)) return favoriteComics.add(comicId);
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
      UUID videoId = ((Video) o).getId();
      return preferences.getFavoriteVideos().remove(videoId);
    } else if (o instanceof Picture) {
      UUID pictureId = ((Picture) o).getId();
      return preferences.getFavoritePictures().remove(pictureId);
    } else if (o instanceof ComicBook) {
      UUID comicId = ((ComicBook) o).getId();
      return preferences.getFavoriteComics().remove(comicId);
    } else
      throw new IllegalArgumentException(
          "Cannot unfavorite unknown type: " + o.getClass().getName());
  }

  public boolean toggleFavorite(@NotNull Video video) {
    Set<UUID> favoriteVideos = getCurrentUserPreferences().getFavoriteVideos();
    return toggleFavorite(favoriteVideos, video.getId());
  }

  public boolean toggleFavorite(@NotNull Picture picture) {
    Set<UUID> favoritePictures = getCurrentUserPreferences().getFavoritePictures();
    return toggleFavorite(favoritePictures, picture.getId());
  }

  public boolean toggleFavorite(@NotNull ComicBook comicBook) {
    Set<UUID> favoriteComics = getCurrentUserPreferences().getFavoriteComics();
    return toggleFavorite(favoriteComics, comicBook.getId());
  }
}
