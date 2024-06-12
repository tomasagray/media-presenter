package net.tomasbot.mp.api.service;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.db.VideoRepository;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Video;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VideoService {

  private static final Logger logger = LogManager.getLogger(VideoService.class);

  private final VideoRepository videoRepository;

  public VideoService(VideoRepository videoRepository) {
    this.videoRepository = videoRepository;
  }

  @NotNull
  private static URL toUrl(@NotNull URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void save(@NotNull Video video) {
    videoRepository.save(video);
  }

  public void saveAll(@NotNull Iterable<? extends Video> videos) {
    videoRepository.saveAll(videos);
  }

  public Page<Video> getAll(int page, int pageSize) {
    return videoRepository.findAll(PageRequest.of(page, pageSize));
  }

  public Page<Video> getLatest(int page, int pageSize) {
    return videoRepository.findAllByOrderByAddedDesc(PageRequest.of(page, pageSize));
  }

  public List<Video> getRandom(int count) {
    return videoRepository.findRandom(PageRequest.ofSize(count));
  }

  public List<Video> getUnprocessedVideos() {
    return videoRepository.findUnprocessedVideos();
  }

  public Optional<Video> getById(@NotNull UUID videoId) {
    return videoRepository.findById(videoId);
  }

  public UrlResource getVideoData(@NotNull UUID videoId) throws MalformedURLException {
    logger.trace("Reading video data for: {}", videoId);
    Optional<Video> videoOptional = getById(videoId);
    if (videoOptional.isPresent()) {
      Video video = videoOptional.get();
      return new UrlResource(video.getFile().toUri());
    }
    throw new IllegalArgumentException("Video not found: " + videoId);
  }

  public Optional<Video> getVideoByPath(@NotNull Path path) {
    return videoRepository.findAll().stream()
        .filter(video -> video.getFile().equals(path))
        .findFirst();
  }

  public void deleteVideo(@NotNull Video video) {
    logger.info("Deleting Video: {}", video);
    videoRepository.delete(video);
  }

  public UrlResource getVideoThumb(@NotNull UUID videoId, @NotNull UUID thumbId) {
    return getById(videoId)
        .map(Video::getThumbnails)
        .map(thumbs -> thumbs.getImage(thumbId))
        .map(Image::getUri)
        .map(VideoService::toUrl)
        .map(UrlResource::new)
        .orElseThrow();
  }
}
