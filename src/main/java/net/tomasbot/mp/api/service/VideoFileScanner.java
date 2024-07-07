package net.tomasbot.mp.api.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import net.tomasbot.mp.model.Tag;
import net.tomasbot.mp.model.Video;
import net.tomasbot.mp.plugin.ffmpeg.metadata.FFmpegMetadata;
import net.tomasbot.mp.plugin.ffmpeg.metadata.FFmpegStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class VideoFileScanner implements FileMetadataScanner<Video> {

  private static final Logger logger = LogManager.getLogger(VideoFileScanner.class);

  private final VideoService videoService;
  private final ThumbnailService thumbnailService;
  private final TagService tagService;
  private final TranscodingService transcodingService;

  @Value("${videos.storage-location}")
  private Path videoStorageLocation;

  public VideoFileScanner(
      VideoService videoService,
      ThumbnailService thumbnailService,
      TagService tagService,
      TranscodingService transcodingService) {
    this.videoService = videoService;
    this.thumbnailService = thumbnailService;
    this.tagService = tagService;
    this.transcodingService = transcodingService;
  }

  @Override
  @Async("fileScanner")
  public void scanFileMetadata(@NotNull Video video) {
    try {
      final Path file = video.getFile();

      // metadata
      final FFmpegMetadata metadata = transcodingService.getVideoMetadata(video);
      List<FFmpegStream> streams = metadata.getStreams();
      if (streams == null || streams.isEmpty()) {
        String msg = String.format("Could not read video metadata for %s: no streams", file);
        throw new IOException(msg);
      }
      video.setMetadata(metadata);

      // title
      final String title = transcodingService.getTitle(metadata);
      if (title != null) video.setTitle(title);
      else video.setTitle(FilenameUtils.getBaseName(file.toString()));

      setVideoTags(video);

      // thumbnails
      videoService.save(video); // ensure video has ID
      thumbnailService.generateVideoThumbnails(video);
      videoService.save(video);
    } catch (IOException e) {
      logger.error("Could not scan Video metadata: {}", e.getMessage(), e);
    }
  }

  private synchronized void setVideoTags(@NotNull Video video) {
    Path tagPath = videoStorageLocation.relativize(video.getFile());
    final List<Tag> tags = tagService.getTags(tagPath);
    video.setTags(new HashSet<>(tags));
  }
}
