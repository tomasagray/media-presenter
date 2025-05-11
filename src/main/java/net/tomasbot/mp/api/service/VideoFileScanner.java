package net.tomasbot.mp.api.service;

import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegStream;
import net.tomasbot.mp.model.Tag;
import net.tomasbot.mp.model.Video;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
public class VideoFileScanner implements FileMetadataScanner<Video> {

  private static final Logger logger = LogManager.getLogger();

  private final VideoService videoService;
  private final ThumbnailService thumbnailService;
  private final PathTagService pathTagService;
  private final TranscodingService transcodingService;
  private final InvalidFilesService invalidFilesService;

  @Value("${videos.storage-location}")
  private Path videoStorageLocation;

  public VideoFileScanner(
          VideoService videoService,
          ThumbnailService thumbnailService,
          PathTagService pathTagService,
          TranscodingService transcodingService, InvalidFilesService invalidFilesService) {
    this.videoService = videoService;
    this.thumbnailService = thumbnailService;
    this.pathTagService = pathTagService;
    this.transcodingService = transcodingService;
    this.invalidFilesService = invalidFilesService;
  }

  @Override
  @Async("fileScanner")
  public void scanFileMetadata(@NotNull Video video) {
    final Path file = video.getFile();

    try {

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
    } catch (Throwable e) {
      logger.error("Could not scan Video metadata: {}", e.getMessage());
      logger.debug(e);
      invalidFilesService.addInvalidFile(file, Video.class);
    }
  }

  private synchronized void setVideoTags(@NotNull Video video) {
    Path tagPath = videoStorageLocation.relativize(video.getFile());
    final List<Tag> tags = pathTagService.getTagsFrom(tagPath);
    video.getTags().addAll(tags);
  }
}
