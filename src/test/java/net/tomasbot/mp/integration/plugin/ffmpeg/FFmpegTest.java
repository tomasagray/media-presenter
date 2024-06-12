package net.tomasbot.mp.integration.plugin.ffmpeg;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import net.tomasbot.mp.plugin.ffmpeg.FFmpeg;
import net.tomasbot.mp.plugin.ffmpeg.FFmpegStreamTask;
import net.tomasbot.mp.plugin.ffmpeg.SimpleTranscodeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Validation for FFMpeg wrapper")
class FFmpegTest {

	private static final Logger logger = LogManager.getLogger(FFmpegTest.class);

	private final FFmpeg ffmpeg;

	FFmpegTest() {
		this.ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
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
		logger.info("Transcoding video: {} to {} with codecs: {}",
				input, output, String.format("%s/%s", videoCodec, audioCodec));
		SimpleTranscodeRequest request = SimpleTranscodeRequest.builder()
				.from(input)
				.to(output)
				.audioCodec(audioCodec)
				.videoCodec(videoCodec)
				.additionalArgs(Map.of("-metadata", "title=some-title"))
				.build();
		final FFmpegStreamTask transcodeTask = ffmpeg.getTranscodeTask(request);
		transcodeTask.start();
		transcodeTask.onLoggableEvent(line -> System.out.println("log: " + line));
		int exitCode = transcodeTask.getProcess().waitFor();
		logger.info("Transcode ended with exit code: {}", exitCode);

		// then
		assertThat(exitCode).isZero();
	}
}