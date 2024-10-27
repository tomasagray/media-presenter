package net.tomasbot.mp.api.service.user;

import static net.tomasbot.mp.user.UserVideoView.UserVideoModeller;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.api.service.VideoService;
import net.tomasbot.mp.model.UserPreferences;
import net.tomasbot.mp.model.Video;
import net.tomasbot.mp.user.UserVideoView;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserVideoService {

  private final VideoService videoService;
  private final UserPreferenceService userPreferenceService;
  private final UserVideoModeller videoModeller;

  public UserVideoService(
      VideoService videoService,
      UserPreferenceService userPreferenceService,
      UserVideoModeller videoModeller) {
    this.videoService = videoService;
    this.userPreferenceService = userPreferenceService;
    this.videoModeller = videoModeller;
  }

  public Page<UserVideoView> getAllUserVideos(int page, int size) {
    return videoService.getAll(page, size).map(this::getUserVideoView);
  }

  public Page<UserVideoView> getLatestUserVideos(int page, int size) {
    return videoService.getLatest(page, size).map(this::getUserVideoView);
  }

  public Optional<UserVideoView> getUserVideo(@NotNull UUID videoId) {
    return videoService.getVideo(videoId).map(this::getUserVideoView);
  }

  public List<UserVideoView> getRandomUserVideos(int count) {
    return videoService.getRandom(count).stream().map(this::getUserVideoView).toList();
  }

  public UserVideoView getUserVideoView(@NotNull Video video) {
    return userPreferenceService.isFavorite(video)
        ? videoModeller.toFavorite(video)
        : videoModeller.toView(video);
  }

  public Collection<UserVideoView> getUserVideoViews(@NotNull Collection<Video> videos) {
    return videos.stream().map(this::getUserVideoView).toList();
  }

  public UserVideoView toggleVideoFavorite(@NotNull UUID videoId) {
    Optional<Video> optional = videoService.getVideo(videoId);
    if (optional.isEmpty()) {
      throw new IllegalArgumentException(
          "Trying to set favorite on non-existent Video: " + videoId);
    }
    Video video = optional.get();
    if (userPreferenceService.toggleFavorite(video)) {
      return videoModeller.toFavorite(video);
    }
    return videoModeller.toView(video);
  }

  public Collection<UserVideoView> getVideoFavorites() {
    return userPreferenceService.getCurrentUserPreferences().getFavoriteVideos().stream()
        .map(videoService::getVideo)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::getUserVideoView)
        .toList();
  }

  public void unfavoriteForAllUsers(Video video) {
    List<UserPreferences> allPreferences = userPreferenceService.getAllUserPreferences();
    for (UserPreferences preferences : allPreferences) {
      userPreferenceService.removeFavorite(preferences, video);
    }
  }

  // === Passthru methods ===
  public UrlResource getVideoData(@NotNull UUID videoId) throws MalformedURLException {
    return videoService.getVideoData(videoId);
  }

  public UrlResource getVideoThumb(@NotNull UUID videoId, @NotNull UUID thumbId) {
    return videoService.getVideoThumb(videoId, thumbId);
  }

  public UserVideoView updateVideo(@NotNull UserVideoView videoView) {
    Video video = videoModeller.fromView(videoView);
    Video updated = videoService.updateVideo(video);
    return videoModeller.toView(updated);
  }
}
