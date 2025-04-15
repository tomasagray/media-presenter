package net.tomasbot.mp.api.service.user;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.tomasbot.mp.api.service.ComicBookService;
import net.tomasbot.mp.api.service.PictureService;
import net.tomasbot.mp.api.service.VideoService;
import net.tomasbot.mp.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserDataService {

  private static final Logger logger = LogManager.getLogger(UserDataService.class);

  private final UserPreferenceService preferenceService;
  private final VideoService videoService;
  private final PictureService pictureService;
  private final ComicBookService comicService;

  public UserDataService(
      UserPreferenceService preferenceService,
      VideoService videoService,
      PictureService pictureService,
      ComicBookService comicService) {
    this.preferenceService = preferenceService;
    this.videoService = videoService;
    this.pictureService = pictureService;
    this.comicService = comicService;
  }

  private static <T> @NotNull Collection<T> lookupFavorite(
      @NotNull Function<UUID, Optional<? extends T>> idLookup,
      @NotNull Function<Path, Collection<T>> pathLookup,
      @NotNull UserData.Favorite fav) {
    Optional<? extends T> optional = idLookup.apply(fav.id());
    if (optional.isPresent()) {
      return List.of(optional.get());
    } else {
      return pathLookup.apply(fav.path());
    }
  }

  public UserData getUserData(String username) {
    UserPreferences preferences = preferenceService.getUserPreferences(username);
    final UserData userData = new UserData(username);

    // add video favs
    preferences.getFavoriteVideos().stream()
        .map(Favorite::getEntityId)
        .forEach(
            videoId -> {
              Optional<Video> videoOptional = videoService.getVideo(videoId);
              if (videoOptional.isPresent()) {
                Video video = videoOptional.get();
                UserData.Favorite favorite =
                    new UserData.Favorite(videoId, video.getTitle(), video.getFile());
                userData.getFavoriteVideos().add(favorite);
              } else
                throw new IllegalArgumentException(
                    "Cannot get user data; invalid Video favorite: " + videoId);
            });

    // add picture favs
    preferences.getFavoritePictures().stream()
        .map(Favorite::getEntityId)
        .forEach(
            pictureId -> {
              Optional<Picture> pictureOptional = pictureService.getPicture(pictureId);
              if (pictureOptional.isPresent()) {
                Picture picture = pictureOptional.get();
                Path path = Path.of(picture.getUri());
                UserData.Favorite favorite =
                    new UserData.Favorite(pictureId, picture.getTitle(), path);
                userData.getFavoritePictures().add(favorite);
              } else
                throw new IllegalArgumentException(
                    "Cannot get user data; invalid Picture favorite: " + pictureId);
            });

    // add comic favs
    preferences.getFavoriteComics().stream()
        .map(Favorite::getEntityId)
        .forEach(
            comicId -> {
              Optional<ComicBook> comicOptional = comicService.getComicBook(comicId);
              if (comicOptional.isPresent()) {
                ComicBook comic = comicOptional.get();
                UserData.Favorite favorite =
                    new UserData.Favorite(comicId, comic.getTitle(), comic.getLocation());
                userData.getFavoriteComics().add(favorite);
              } else
                throw new IllegalArgumentException(
                    "Cannot get user data; invalid ComicBook favorite: " + comicId);
            });

    return userData;
  }

  public void importUserData(@NotNull UserData userData) {
    final String username = userData.getUsername();
    validateImportRequest(username);

    logger.info("Importing UserPreferences for user: {}", username);

    setFavorites(
        userData.getFavoriteVideos(), videoService::getVideo, videoService::getVideoByPath);
    setFavorites(
        userData.getFavoritePictures(),
        pictureService::getPicture,
        pictureService::getPictureByPath);
    setFavorites(
        userData.getFavoriteComics(), comicService::getComicBook, comicService::getComicBooksAt);

    logger.info("Successfully imported UserPreferences for user: {}", username);
  }

  private <T> void setFavorites(
      @NotNull Collection<UserData.Favorite> favorites,
      @NotNull Function<UUID, Optional<? extends T>> idLookup,
      @NotNull Function<Path, Collection<T>> pathLookup) {
    favorites.stream()
        .map(fav -> lookupFavorite(idLookup, pathLookup, fav))
        .flatMap(Collection::stream)
        .forEach(preferenceService::setFavorite);
  }

  private void validateImportRequest(@NotNull String username) {
    UserPreferences currentUserPreferences = preferenceService.getCurrentUserPreferences();
    String currentUsername = currentUserPreferences.getUsername();
    if (!username.equals(currentUsername)) {
      String msg =
          String.format(
              "%s is attempting to import user data from another user: %s",
              currentUsername, username);
      throw new SecurityException(msg);
    }
  }
}
