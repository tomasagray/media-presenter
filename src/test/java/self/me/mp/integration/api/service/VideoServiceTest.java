package self.me.mp.integration.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.mp.api.service.VideoService;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("VideoService unit tests")
public class VideoServiceTest {

	private static final Logger logger = LogManager.getLogger(VideoServiceTest.class);

	private final VideoService videoService;

	@Autowired
	public VideoServiceTest(VideoService videoService) {
		this.videoService = videoService;
	}

	@Test
	@DisplayName("Validate time to scan 5,000+ files")
	void testScanningManyFilesTime() throws IOException {

		// given
		int maxDuration = 500;
		logger.info("Beginning test scan...");
		final Instant start = Instant.now();

		// when
		videoService.init(() -> {
			Instant end = Instant.now();
			Duration duration = Duration.between(start, end);
			logger.info("Scanning took: {}ms", duration.toMillis());
			// then
			assertFalse(duration.toMillis() > maxDuration);
		});
	}
}
