package net.tomasbot.mp.api.service.user;

import java.util.Optional;
import net.tomasbot.mp.api.service.ComicBookService;
import net.tomasbot.mp.api.service.PictureService;
import net.tomasbot.mp.api.service.VideoService;
import net.tomasbot.mp.model.UserData;
import net.tomasbot.mp.model.UserPreferences;
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

  public UserData getUserData(String username) {
    UserPreferences preferences = preferenceService.getUserPreferences(username);
    final UserData userData = new UserData(username);
    // add video favs
    preferences
        .getFavoriteVideos()
        .forEach(
            video -> {
              UserData.Favorite favorite = new UserData.Favorite(video.getId(), video.getTitle());
              userData.getFavoriteVideos().add(favorite);
            });
    // add picture favs
    preferences
        .getFavoritePictures()
        .forEach(
            pic -> {
              UserData.Favorite favorite = new UserData.Favorite(pic.getId(), pic.getTitle());
              userData.getFavoritePictures().add(favorite);
            });
    // add comic favs
    preferences
        .getFavoriteComics()
        .forEach(
            comic -> {
              UserData.Favorite favorite = new UserData.Favorite(comic.getId(), comic.getTitle());
              userData.getFavoriteComics().add(favorite);
            });
    return userData;
  }

  public void importUserData(@NotNull UserData userData) {
    final String username = userData.getUsername();
    logger.info("Importing UserPreferences for user: {}", username);

    UserPreferences preferences = preferenceService.getUserPreferences(username);
    userData.getFavoriteVideos().stream()
        .map(UserData.Favorite::id)
        .map(videoService::getVideo)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(preferenceService::setFavorite);
    userData.getFavoritePictures().stream()
        .map(UserData.Favorite::id)
        .map(pictureService::getPicture)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(preferenceService::setFavorite);
    userData.getFavoriteComics().stream()
        .map(UserData.Favorite::id)
        .map(comicService::getComicBook)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(preferenceService::setFavorite);

    logger.info("Successfully imported UserPreferences for user: {}", username);
  }
}
