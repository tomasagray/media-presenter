package self.me.mp.init;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import self.me.mp.api.service.RecursiveWatcherService;
import self.me.mp.api.service.VideoScanningService;

@Component
@Order(2)
@Profile("!test-volatile")
public class WatchVideoAddDirectory implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(WatchVideoAddDirectory.class);
	private final RecursiveWatcherService watcherService;
	private final VideoScanningService videoScanningService;
	@Value("${videos.add-location}")
	private Path addVideoLocation;

	public WatchVideoAddDirectory(RecursiveWatcherService watcherService, VideoScanningService videoScanningService) {
		this.watcherService = watcherService;
		this.videoScanningService = videoScanningService;
	}

	private static void initializeVideoLocation(@NotNull Path location) throws IOException {
		File file = location.toFile();
		if (!file.exists()) {
			logger.info("Video storage location: {} does not exist; creating...", location);
			if (!file.mkdirs()) {
				throw new IOException("Could not create location for Video storage: " + location);
			}
		}
	}

	@Override
	@Async("startup")
	public void run(String... args) throws Exception {
		initializeVideoLocation(addVideoLocation);

		logger.info("Initializing add new video watcher in: {}", addVideoLocation);
		Instant jobStart = Instant.now();
		watcherService.watch(
				addVideoLocation,
				videoScanningService::scanAddFile,
				videoScanningService::handleAddVideoEvent,
				() -> {
					Duration jobDuration = Duration.between(jobStart, Instant.now());
					logger.info("Initial scan of add new video directory completed in {}ms", jobDuration.toMillis());
				}
		);
	}
}
