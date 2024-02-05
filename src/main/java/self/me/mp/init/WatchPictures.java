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
import self.me.mp.api.service.PictureScanningService;
import self.me.mp.api.service.PictureService;
import self.me.mp.api.service.RecursiveWatcherService;
import self.me.mp.model.Picture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(2)
@Profile("!test-volatile")
public class WatchPictures implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(WatchPictures.class);
	private final RecursiveWatcherService watcherService;
	private final PictureScanningService scanningService;
	private final PictureService pictureService;
	@Value("${pictures.location}")
	private Path pictureLocation;

	public WatchPictures(
			RecursiveWatcherService watcherService,
			PictureScanningService scanningService,
			PictureService pictureService) {
		this.watcherService = watcherService;
		this.scanningService = scanningService;
		this.pictureService = pictureService;
	}

	private void initializePictureLocation() throws IOException {
		File file = pictureLocation.toFile();
		if (!file.exists()) {
			logger.info("Picture storage location: {} does not exist; creating...", pictureLocation);
			if (!file.mkdirs()) {
				throw new IOException("Could not create location for Picture storage: " + pictureLocation);
			}
		}
	}

	@Override
	@Async("startup")
	public void run(String... args) throws Exception {
		initializePictureLocation();
		Set<Path> existing = pictureService.getAll(0, Integer.MAX_VALUE)
				.stream()
				.map(Picture::getUri)
				.map(Paths::get)
				.collect(Collectors.toSet());

		logger.info("Scanning Picture files in: {}", pictureLocation);
		final Instant jobStart = Instant.now();
		watcherService.watch(
				pictureLocation,
				file -> scanningService.scanFile(file, existing),
				scanningService::handleFileEvent,
				() -> finishPictureScan(jobStart)
		);
	}

	private void finishPictureScan(@NotNull Instant jobStart) {
		Duration jobDuration = Duration.between(jobStart, Instant.now());
		logger.info("Initial scan of Pictures finished in {}ms", jobDuration.toMillis());
		scanningService.saveScannedData();
		List<Picture> pictures = pictureService.getUnprocessedPictures();
		for (Picture picture : pictures) {
			scanningService.processImageMetadata(picture);
		}
	}
}
