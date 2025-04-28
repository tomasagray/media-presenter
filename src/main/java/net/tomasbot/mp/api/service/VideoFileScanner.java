package net.tomasbot.mp.api.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.tomasbot.mp.model.Tag;
import net.tomasbot.mp.model.Video;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegStream;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class VideoFileScanner implements FileMetadataScanner<Video> {

  private final VideoService videoService;
  private final ThumbnailService thumbnailService;
  private final PathTagService pathTagService;
  private final TranscodingService transcodingService;

  @Value("${videos.storage-location}")
  private Path videoStorageLocation;

  public VideoFileScanner(
      VideoService videoService,
      ThumbnailService thumbnailService,
      PathTagService pathTagService,
      TranscodingService transcodingService) {
    this.videoService = videoService;
    this.thumbnailService = thumbnailService;
    this.pathTagService = pathTagService;
    this.transcodingService = transcodingService;
  }

  @Override
  @Async("fileScanner")
  public void scanFileMetadata(@NotNull Video video) throws IOException {
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
  }

  private synchronized void setVideoTags(@NotNull Video video) {
    Path tagPath = videoStorageLocation.relativize(video.getFile());
    final List<Tag> tags = pathTagService.getTagsFrom(tagPath);
    video.getTags().addAll(tags);
  }
}
