package net.tomasbot.mp.api.service.user;

import net.tomasbot.mp.api.service.PictureService;
import net.tomasbot.mp.api.service.RandomPictureService;
import net.tomasbot.mp.model.Favorite;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.user.UserImageView;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static net.tomasbot.mp.user.UserImageView.UserImageModeller;

@Service
@Transactional
public class UserPictureService {

  private final UserPreferenceService userPreferenceService;
  private final PictureService pictureService;
  private final RandomPictureService randomPictureService;
  private final UserImageModeller modeller;

  public UserPictureService(
          UserPreferenceService userPreferenceService,
          PictureService pictureService,
          RandomPictureService randomPictureService,
          UserImageModeller modeller) {
    this.userPreferenceService = userPreferenceService;
    this.pictureService = pictureService;
    this.randomPictureService = randomPictureService;
    this.modeller = modeller;
  }

  public Page<UserImageView> getLatestUserPictures(int page, int size) {
    return pictureService.getLatestPictures(page, size).map(this::getUserImageView);
  }

  public List<UserImageView> getRandomUserPictures() {
    List<Picture> randomCollection = randomPictureService.getRandomCollection();
    if (randomCollection.isEmpty())
      randomCollection = pictureService.getRandomPictures(18);
    return randomCollection.stream().map(this::getUserImageView).toList();
  }

  public Optional<UserImageView> getUserPicture(@NotNull UUID picId) {
    return pictureService.getPicture(picId).map(this::getUserImageView);
  }

  public UserImageView getUserImageView(@NotNull Picture picture) {
    return userPreferenceService.isFavorite(picture)
        ? modeller.toFavorite(picture)
        : modeller.toView(picture);
  }

  public Collection<UserImageView> getUserImageViews(@NotNull Collection<Picture> pictures) {
    return pictures.stream().map(this::getUserImageView).toList();
  }

  public UserImageView toggleIsPictureFavorite(@NotNull UUID picId) {
    Optional<Picture> optional = pictureService.getPicture(picId);
    if (optional.isEmpty())
      throw new IllegalArgumentException("Trying to favorite non-existent Picture: " + picId);

    Picture picture = optional.get();
    if (!userPreferenceService.isFavorite(picture)) {
      if (userPreferenceService.setFavorite(picture)) return modeller.toFavorite(picture);
      else throw new SetFavoriteException(picture);
    } else {
      if (userPreferenceService.removeFavorite(picture)) return modeller.toView(picture);
      else throw new RemoveFavoriteException(picture);
    }
  }

  public UserImageView updatePicture(@NotNull UserImageView imageView) {
    Picture picture = (Picture) modeller.fromView(imageView);
    Picture updated = pictureService.updatePicture(picture);
    return modeller.toView(updated);
  }

  public Collection<UserImageView> getFavoritePictures() {
    return userPreferenceService.getCurrentUserPreferences().getFavoritePictures().stream()
        .sorted(Comparator.comparing(Favorite::getTimestamp).reversed())
        .map(Favorite::getEntityId)
        .map(pictureService::getPicture)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::getUserImageView)
        .toList();
  }

  public Optional<UrlResource> getPictureData(UUID picId) {
    return pictureService.getPictureData(picId);
  }
}
