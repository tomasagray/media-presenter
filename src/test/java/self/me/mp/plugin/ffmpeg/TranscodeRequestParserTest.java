package self.me.mp.plugin.ffmpeg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;


@DisplayName("Validation tests for TranscodeRequestParser")
class TranscodeRequestParserTest {

	private static final Logger logger = LogManager.getLogger(TranscodeRequestParserTest.class);

	private static TranscodeRequestParser parser;

	@BeforeAll
	static void setup() {
		parser = new TranscodeRequestParser();
	}

	@Test
	@DisplayName("Test parsing a transcode request into CLI string")
	void testRequestParse() {
		// given
		final String audioCodec = "aac";
		final String videoCodec = "libx264";
		final URI from = URI.create("/test/from");
		final Path to = Path.of("/test/to");
		final String expected =
				String.format("-i %s -c:v %s -c:a %s %s", from, videoCodec, audioCodec, to);

		// when
		final SimpleTranscodeRequest request = SimpleTranscodeRequest.builder()
				.audioCodec(audioCodec)
				.videoCodec(videoCodec)
				.from(from)
				.to(to)
				.build();
		final String cli = parser.parse(request);
		logger.info("Got CLI string:\n\t{}", cli);
		logger.info("Expected:\n\t{}", expected);

		// then
		assert cli != null && !cli.isEmpty();
		assert cli.equals(expected);
	}
}