package self.me.mp.api.service.user;

import static self.me.mp.user.UserVideoView.UserVideoModeller;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.mp.api.service.VideoService;
import self.me.mp.model.UserPreferences;
import self.me.mp.model.Video;
import self.me.mp.user.UserVideoView;

@Service
@Transactional
public class UserVideoService {

	private final VideoService videoService;
	private final UserService userService;
	private final UserVideoModeller videoModeller;

	public UserVideoService(
			VideoService videoService,
			UserService userService,
			UserVideoModeller videoModeller) {
		this.videoService = videoService;
		this.userService = userService;
		this.videoModeller = videoModeller;
	}

	public Page<UserVideoView> getAllUserVideos(int page, int size) {
		return videoService.getAll(page, size).map(this::getUserVideoView);
	}

	public Page<UserVideoView> getLatestUserVideos(int page, int size) {
		return videoService.getLatest(page, size).map(this::getUserVideoView);
	}

	public Optional<UserVideoView> getUserVideo(@NotNull UUID videoId) {
		return videoService.getById(videoId).map(this::getUserVideoView);
	}

	public List<UserVideoView> getRandomUserVideos(int count) {
		return videoService.getRandom(count).stream().map(this::getUserVideoView).toList();
	}

	private UserVideoView getUserVideoView(@NotNull Video video) {
		UserPreferences preferences = userService.getUserPreferences();
		return preferences.isFavorite(video) ?
				videoModeller.toFavorite(video) : videoModeller.toView(video);
	}

	public Collection<UserVideoView> getUserVideoViews(@NotNull Collection<Video> videos) {
		return videos.stream().map(this::getUserVideoView).toList();
	}

	public UserVideoView toggleVideoFavorite(@NotNull UUID videoId) {
		Optional<Video> optional = videoService.getById(videoId);
		if (optional.isEmpty()) {
			throw new IllegalArgumentException("Trying to set favorite on non-existent Video: " + videoId);
		}
		Video video = optional.get();
		UserPreferences preferences = userService.getUserPreferences();
		if (preferences.toggleFavorite(video)) {
			return videoModeller.toFavorite(video);
		}
		return videoModeller.toView(video);
	}

	public Collection<UserVideoView> getVideoFavorites() {
		return userService.getUserPreferences()
				.getFavoriteVideos().stream()
				.map(this::getUserVideoView)
				.toList();
	}

	public UrlResource getVideoData(@NotNull UUID videoId) throws MalformedURLException {
		return videoService.getVideoData(videoId);
	}

	public UrlResource getVideoThumb(@NotNull UUID videoId, @NotNull UUID thumbId) {
		return videoService.getVideoThumb(videoId, thumbId);
	}
}
