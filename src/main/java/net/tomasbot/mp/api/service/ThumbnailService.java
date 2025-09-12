package net.tomasbot.mp.api.service;

import lombok.Getter;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegStream;
import net.tomasbot.mp.db.ImageRepository;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.ImageSet;
import net.tomasbot.mp.model.Video;
import net.tomasbot.mp.plugin.ffmpeg.FFmpegPlugin;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.*;

@Service
public class ThumbnailService {

  private static final Logger logger = LogManager.getLogger(ThumbnailService.class);

  private final ImageRepository imageRepository;
  private final FFmpegPlugin ffmpeg;

  @Getter
  @Value("${video.thumbnails.location}")
  private Path thumbLocation;

  @Value("${video.thumbnails.width}")
  private int defaultWidth;

  @Value("${video.thumbnails.height}")
  private int defaultHeight;

  public ThumbnailService(ImageRepository imageRepository, FFmpegPlugin ffmpeg) {
    this.imageRepository = imageRepository;
    this.ffmpeg = ffmpeg;
  }

  public Optional<Image> getThumbnailAt(@NotNull Path path) {
    List<Image> thumbs = imageRepository.findByUri(path.toUri());

    if (thumbs.isEmpty()) return Optional.empty();
    if (thumbs.size() > 1)
      throw new IllegalArgumentException("Found multiple thumbnail entries for same location: " + path);

    return Optional.of(thumbs.get(0));
  }

  public void generateVideoThumbnails(@NotNull Video video) throws IOException {
    List<FFmpegStream> streams = video.getMetadata().getStreams();
    if (streams == null || streams.isEmpty()) {
      throw new IOException("Video has no streams: " + video);
    }

    // determine thumbnail intervals
    final double duration = streams.get(0).getDuration();
    final int sliceFactor = getSliceFactor(duration);
    final double offset = duration * 0.05;
    final double sliceDuration = duration / sliceFactor;

    logger.info("Creating thumbnails for Video: {}", video);
    Path thumb = createThumbDirFor(video);
    for (int i = 0; i < sliceFactor; i++) {
      double thumbPos = i * sliceDuration + offset;
      Image thumbnail = generateThumbnail(video, thumb.resolve(i + ".jpg"), (long) thumbPos);
      logger.info("Created thumbnail: {}", thumbnail);
      video.addThumbnail(thumbnail);
    }
  }

  public Image generateThumbnail(@NotNull Video video, @NotNull Path thumb, long thumbPos)
      throws IOException {
    Path thumbnail =
        ffmpeg.createThumbnail(
            video.getFile(), thumb, LocalTime.ofSecondOfDay(thumbPos), defaultWidth, defaultHeight);
    Image image = Image.builder()
        .uri(thumbnail.toUri())
        .width(defaultWidth)
        .height(defaultHeight)
        .filesize(thumbnail.toFile().length())
        .title(FilenameUtils.getBaseName(thumbnail.toString()))
        .build();
    return imageRepository.save(image);
  }

  @NotNull
  private Path createThumbDirFor(@NotNull Video video) throws IOException {
    Path thumb = thumbLocation.resolve(video.getId().toString());
    File thumbFile = thumb.toFile();
    if (!thumbFile.mkdirs() && !thumbFile.getParentFile().exists()) {
      throw new IOException("Could not create thumbnail directory: " + thumb);
    }
    return thumb;
  }

  private int getSliceFactor(double duration) {
    if (duration <= 60.0d) {
      return 3;
    } else if (duration <= (3 * 60.0d)) {
      return 5;
    } else {
      return 10;
    }
  }

  @Transactional
  public void deleteThumbs(@NotNull Video video) throws IOException {
    ImageSet thumbnails = video.getThumbnails();
    if (thumbnails != null) {
      Set<Image> images = thumbnails.getImages();
      if (images != null && !images.isEmpty())
        deleteThumbs(images);
    }

    // delete thumbnail dir; assumes dir = videoId
    deleteThumbnailDir(video);
  }

  public void deleteThumbs(@NotNull Collection<Image> images) throws IOException {
    int deleted = 0;
    Iterator<Image> iterator = images.iterator();
    while (iterator.hasNext()) {
      Image thumbnail = iterator.next();
      logger.info("Deleting thumbnail: {}", thumbnail);

      iterator.remove();
      deleteThumbnail(thumbnail);
      deleted++;
    }

    logger.info("Deleted {} thumbnails", deleted);
  }

  public void deleteThumbnail(@NotNull Image thumb) throws IOException {
    File file = Path.of(thumb.getUri()).toFile();
    if (file.exists()) {
      boolean deleted = file.delete();
      if (!deleted) throw new IOException("Could not delete thumbnail file at: " + file);
    }

    imageRepository.delete(thumb);
  }

  private void deleteThumbnailDir(@NotNull Video video) throws IOException {
    Path thumbDir = thumbLocation.resolve(video.getId().toString());
    logger.info("Deleting thumbnail directory at: {}", thumbDir);
    Files.delete(thumbDir);
  }
}
