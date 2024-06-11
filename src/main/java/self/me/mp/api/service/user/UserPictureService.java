package self.me.mp.api.service.user;

import static self.me.mp.user.UserImageView.UserImageModeller;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.mp.api.service.PictureService;
import self.me.mp.model.Picture;
import self.me.mp.model.UserPreferences;
import self.me.mp.user.UserImageView;

@Service
@Transactional
public class UserPictureService {

	private final UserService userService;
	private final PictureService pictureService;
	private final UserImageModeller modeller;

	public UserPictureService(
			UserService userService,
			PictureService pictureService,
			UserImageModeller modeller) {
		this.userService = userService;
		this.pictureService = pictureService;
		this.modeller = modeller;
	}

	public Page<UserImageView> getLatestUserPictures(int page, int size) {
		return pictureService.getLatestPictures(page, size).map(this::getUserImageView);
	}

	public List<UserImageView> getRandomUserPictures(int count) {
		return pictureService.getRandomPictures(count).stream().map(this::getUserImageView).toList();
	}

	public Optional<UserImageView> getUserPicture(@NotNull UUID picId) {
		return pictureService.getPicture(picId).map(this::getUserImageView);
	}

	public UserImageView getUserImageView(@NotNull Picture picture) {
		return userService.getUserPreferences().isFavorite(picture) ?
				modeller.toFavorite(picture) : modeller.toView(picture);
	}

	public Collection<UserImageView> getUserImageViews(@NotNull Collection<Picture> pictures) {
		return pictures.stream().map(this::getUserImageView).toList();
	}

	public UserImageView toggleIsPictureFavorite(@NotNull UUID picId) {
		Optional<Picture> optional = pictureService.getPicture(picId);
		if (optional.isEmpty()) {
			throw new IllegalArgumentException("Trying to favorite non-existent Picture: " + picId);
		}
		Picture picture = optional.get();
		UserPreferences preferences = userService.getUserPreferences();
		if (preferences.toggleFavorite(picture)) {
			return modeller.toFavorite(picture);
		}
		return modeller.toView(picture);
	}

	public Collection<UserImageView> getFavoritePictures() {
		return userService.getUserPreferences()
				.getFavoritePictures().stream()
				.map(this::getUserImageView)
				.toList();
	}

	public Optional<UrlResource> getPictureData(UUID picId) {
		return pictureService.getPictureData(picId);
	}
}
