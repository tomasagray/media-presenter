package net.tomasbot.mp.plugin.ffmpeg.metadata;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Represents audio/video file metadata returned by FFPROBE */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class FFmpegMetadata {

  private FFmpegFormat format;
  private List<FFmpegStream> streams;
  private List<FFmpegChapter> chapters;
}
