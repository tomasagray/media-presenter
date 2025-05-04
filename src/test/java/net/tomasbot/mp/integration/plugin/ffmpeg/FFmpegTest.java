package net.tomasbot.mp.integration.plugin.ffmpeg;

import net.tomasbot.ffmpeg_wrapper.FFmpeg;
import net.tomasbot.ffmpeg_wrapper.request.SimpleTranscodeRequest;
import net.tomasbot.ffmpeg_wrapper.task.FFmpegStreamTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validation for FFMpeg wrapper")
class FFmpegTest {

  private static final Logger logger = LogManager.getLogger(FFmpegTest.class);

  private final FFmpeg ffmpeg;

  FFmpegTest() {
    this.ffmpeg = new FFmpeg("/usr/bin/ffmpeg", List.of(""));
  }

  @Test
  @DisplayName("Ensure transcoding takes place as expected")
  void getTranscodeTask() throws InterruptedException {
    // given
    final URI input = URI.create("/projectdata/mp/test/test_data/videos/0/0.mp4");
    final Path output =
        Path.of("/projectdata/mp/test/test_data/videos/transcode/" + UUID.randomUUID() + ".mp4");
    final String videoCodec = "libx264";
    final String audioCodec = "aac";

    // when
    logger.info(
        "Transcoding video: {} to {} with codecs: {}",
        input,
        output,
        String.format("%s/%s", videoCodec, audioCodec));
    SimpleTranscodeRequest request =
        SimpleTranscodeRequest.builder()
            .from(input)
            .to(output)
            .audioCodec(audioCodec)
            .videoCodec(videoCodec)
            .onEvent(logger::info)
            .additionalArgs(Map.of("-metadata", "title=some-title"))
            .build();

    final FFmpegStreamTask transcodeTask = ffmpeg.getTranscodeTask(request);
    transcodeTask.start();

    logger.info("Waiting for transcode to complete...");
    TimeUnit.SECONDS.sleep(10);

    int exitCode = transcodeTask.getProcess().waitFor();
    logger.info("Transcode ended with exit code: {}", exitCode);

    // then
    assertThat(exitCode).isZero();
  }
}
