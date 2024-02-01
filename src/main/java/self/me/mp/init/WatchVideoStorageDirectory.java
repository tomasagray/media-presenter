package self.me.mp.init;

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
import self.me.mp.api.service.VideoService;
import self.me.mp.model.Video;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Order(2)
@Profile("!test-volatile")
public class WatchVideoStorageDirectory implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(WatchVideoStorageDirectory.class);
	private final RecursiveWatcherService watcherService;
	private final VideoScanningService scanningService;
	private final VideoService videoService;
	@Value("${videos.storage-location}")
	private Path videoStorageLocation;

	public WatchVideoStorageDirectory(
			RecursiveWatcherService watcherService,
			VideoScanningService scanningService,
			VideoService videoService) {
		this.watcherService = watcherService;
		this.scanningService = scanningService;
		this.videoService = videoService;
	}

	private static void initializeVideoStorageLocation(@NotNull Path location) throws IOException {
		File file = location.toFile();
		if (!file.exists()) {
			logger.info("Video storage location: {} does not exist; creating...", location);
			if (!file.mkdirs()) {
				throw new IOException("Could not create location for Video storage: " + location);
			}
		}
	}

	private void finishVideoScan(Instant jobStart) {
		Duration jobDuration = Duration.between(jobStart, Instant.now());
		logger.info("Initial scan of Videos finished in {}ms", jobDuration.toMillis());
		scanningService.saveScannedData();
		List<Video> videos = videoService.getUnprocessedVideos();
		for (Video video : videos) {
			scanningService.scanVideoMetadata(video);
		}
	}

	@Override
	@Async("startup")
	public void run(String... args) throws Exception {
		initializeVideoStorageLocation(videoStorageLocation);
		List<Path> existing = videoService.getAll(0, Integer.MAX_VALUE)
				.stream()
				.map(Video::getFile)
				.toList();

		logger.info("Initializing video storage watcher at: {}", videoStorageLocation);
		final Instant jobStart = Instant.now();
		watcherService.watch(
				videoStorageLocation,
				path -> scanningService.scanFile(path, existing),
				scanningService::handleFileEvent,
				() -> finishVideoScan(jobStart)
		);
	}
}
