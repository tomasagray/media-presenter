package net.tomasbot.mp.api.service.user;

import java.util.*;
import java.util.stream.Collectors;
import net.tomasbot.mp.db.FavoriteRepository;
import net.tomasbot.mp.db.UserPreferencesRepository;
import net.tomasbot.mp.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferenceService {

  private final Logger logger = LogManager.getLogger(UserPreferenceService.class);

  private final UserPreferencesRepository repository;
  private final FavoriteRepository favoriteRepository;

  public UserPreferenceService(
      UserPreferencesRepository repository, FavoriteRepository favoriteRepository) {
    this.repository = repository;
    this.favoriteRepository = favoriteRepository;
  }

  private static @NotNull Set<UUID> getEntityIdsFrom(@NotNull Set<? extends Favorite> favorites) {
    return favorites.stream().map(Favorite::getEntityId).collect(Collectors.toSet());
  }

  private static @Nullable Favorite getRemovalFavorite(
      @NotNull Set<Favorite> favorites, UUID entityId) {
    Favorite remove = null;
    if (entityId == null) return null;

    for (Favorite favorite : favorites) {
      if (entityId.equals(favorite.getEntityId())) {
        remove = favorite;
        break;
      }
    }

    return remove;
  }

  @Transactional
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

  public boolean isFavorite(Video video) {
    return getCurrentUserPreferences().getFavoriteVideos().stream()
        .anyMatch(favorite -> favorite.getEntityId().equals(video.getId()));
  }

  public boolean isFavorite(Picture picture) {
    return getCurrentUserPreferences().getFavoritePictures().stream()
        .anyMatch(favorite -> favorite.getEntityId().equals(picture.getId()));
  }

  public boolean isFavorite(ComicBook comic) {
    return getCurrentUserPreferences().getFavoriteComics().stream()
        .anyMatch(favorite -> favorite.getEntityId().equals(comic.getId()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
  public boolean setFavorite(Object o) {
    return setFavorite(getCurrentUserPreferences(), o);
  }

  public boolean setFavorite(@NotNull UserPreferences preferences, Object o) {
    logger.info("Setting {} as favorite for user: {}", o, preferences.getUsername());

    if (o instanceof Video video) {
      Set<Favorite> favoriteVideos = preferences.getFavoriteVideos();
      return setEntityFavorite(favoriteVideos, video);
    } else if (o instanceof Picture picture) {
      Set<Favorite> favoritePictures = preferences.getFavoritePictures();
      return setEntityFavorite(favoritePictures, picture);
    } else if (o instanceof ComicBook comic) {
      Set<Favorite> favoriteComics = preferences.getFavoriteComics();
      return setEntityFavorite(favoriteComics, comic);
    } else
      throw new IllegalArgumentException("Cannot favorite unknown type: " + o.getClass().getName());
  }

  private boolean setEntityFavorite(@NotNull Set<Favorite> favorites, @NotNull Editable entity) {
    Set<UUID> favoriteEntityIds = getEntityIdsFrom(favorites);
    UUID entityId = entity.getId();

    if (!favoriteEntityIds.contains(entityId)) {
      Favorite favorite = favoriteRepository.save(new Favorite(entityId));
      return favorites.add(favorite);
    } else return false;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
  public boolean removeFavorite(Object o) {
    return removeFavorite(getCurrentUserPreferences(), o);
  }

  public boolean removeFavorite(@NotNull UserPreferences preferences, Object o) {
    logger.info("Removing user [{}] favorite from {}", preferences.getUsername(), o);

    if (o instanceof Video video) {
      UUID videoId = video.getId();
      Set<Favorite> favoriteVideos = preferences.getFavoriteVideos();

      Favorite remove = getRemovalFavorite(favoriteVideos, videoId);
      return remove != null && favoriteVideos.remove(remove);
    } else if (o instanceof Picture picture) {
      UUID pictureId = picture.getId();
      Set<Favorite> favoritePictures = preferences.getFavoritePictures();

      Favorite remove = getRemovalFavorite(favoritePictures, pictureId);
      return remove != null && favoritePictures.remove(remove);
    } else if (o instanceof ComicBook comic) {
      UUID comicId = comic.getId();
      Set<Favorite> favoriteComics = preferences.getFavoriteComics();

      Favorite remove = getRemovalFavorite(favoriteComics, comicId);
      return remove != null && favoriteComics.remove(remove);
    } else
      throw new IllegalArgumentException(
          "Cannot unfavorite unknown type: " + o.getClass().getName());
  }
}
