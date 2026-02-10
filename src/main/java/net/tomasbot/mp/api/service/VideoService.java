package net.tomasbot.mp.api.service;

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class VideoService {

  private static final Logger logger = LogManager.getLogger(VideoService.class);

  private final VideoRepository videoRepository;
  private final TagManagementService tagService;
  private final ThumbnailService thumbnailService;

  public VideoService(
          VideoRepository videoRepository, TagManagementService tagService, ThumbnailService thumbnailService) {
    this.videoRepository = videoRepository;
    this.tagService = tagService;
    this.thumbnailService = thumbnailService;
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
    videoRepository.saveAndFlush(video);
  }

  public void saveAll(@NotNull Iterable<? extends Video> videos) {
    videoRepository.saveAll(videos);
  }

  public List<Video> getAll() {
    return videoRepository.findAll();
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

  public Optional<Video> getVideo(@NotNull UUID videoId) {
    return videoRepository.findById(videoId);
  }

  public UrlResource getVideoData(@NotNull UUID videoId) throws MalformedURLException {
    logger.trace("Reading video data for: {}", videoId);
    Optional<Video> videoOptional = getVideo(videoId);
    if (videoOptional.isPresent()) {
      Video video = videoOptional.get();
      return new UrlResource(video.getFile().toUri());
    }
    throw new IllegalArgumentException("Video not found: " + videoId);
  }

  public List<Video> getVideoByPath(@NotNull Path path) {
    return videoRepository.findAllByFile(path);
  }

  public void deleteVideo(@NotNull Video video) throws IOException {
    logger.info("Deleting Video: {}", video);
    thumbnailService.deleteThumbs(video);
    videoRepository.delete(video);
  }

  public UrlResource getVideoThumb(@NotNull UUID videoId, @NotNull UUID thumbId) {
    return getVideo(videoId)
        .map(Video::getThumbnails)
        .map(thumbs -> thumbs.getImage(thumbId))
        .map(Image::getUri)
        .map(VideoService::toUrl)
        .map(UrlResource::new)
        .orElseThrow();
  }

  /**
   * Update the video stored in the database. Right now, only updates tags & title
   *
   * @param update The updated video; assumed to be incomplete
   * @return The updated, complete video
   */
  public Video updateVideo(@NotNull Video update) {
    logger.info("Updating Video: {}", update);

    final UUID videoId = update.getId();
    return (Video)
        videoRepository
            .findById(videoId)
            .map(existing -> tagService.update(existing, update))
            .orElseThrow(
                () -> new IllegalArgumentException("Cannot update non-existent video: " + videoId));
  }
}
