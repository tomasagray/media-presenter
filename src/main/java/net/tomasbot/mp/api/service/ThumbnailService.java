package net.tomasbot.mp.api.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Video;
import net.tomasbot.mp.plugin.ffmpeg.FFmpegPlugin;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ThumbnailService {

  private static final Logger logger = LogManager.getLogger(ThumbnailService.class);

  private final FFmpegPlugin ffmpeg;

  @Value("${video.thumbnails.location}")
  private Path thumbLocation;

  @Value("${video.thumbnails.width}")
  private int defaultWidth;

  @Value("${video.thumbnails.height}")
  private int defaultHeight;

  public ThumbnailService(FFmpegPlugin ffmpeg) {
    this.ffmpeg = ffmpeg;
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
    return Image.builder()
        .uri(thumbnail.toUri())
        .width(defaultWidth)
        .height(defaultHeight)
        .filesize(thumbnail.toFile().length())
        .title(FilenameUtils.getBaseName(thumbnail.toString()))
        .build();
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
}
