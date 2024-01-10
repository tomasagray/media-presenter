package self.me.mp.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import self.me.mp.api.service.ComicBookService;
import self.me.mp.api.service.PictureService;
import self.me.mp.api.service.RecursiveWatcherService;
import self.me.mp.api.service.VideoService;

import java.io.IOException;

@Component
@Order(2)
@Profile("!test-volatile")
public class StartWatchingFileSystem implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(StartWatchingFileSystem.class);

	private final RecursiveWatcherService watcherService;
	private final VideoService videoService;
	private final PictureService pictureService;
	private final ComicBookService comicService;

	public StartWatchingFileSystem(
			RecursiveWatcherService watcherService,
			VideoService videoService,
			PictureService pictureService,
			ComicBookService comicService) {
		this.watcherService = watcherService;
		this.videoService = videoService;
		this.pictureService = pictureService;
		this.comicService = comicService;
	}

	@Override
	public void run(String... args) throws IOException {
		logger.info("Starting filesystem WatchService...");
		watcherService.doWatch();
		logger.info("Filesystem WatchService started.");

		// perform initial file scans...
		logger.info("Performing initial filesystem scans. This may take a while...");
		videoService.initAddVideoDirectory(() -> logger.info("Finished initial scan of video 'add' directory..."));
		videoService.initVideoStorageLocation(() -> logger.info("Finished initial scan of video 'storage' directory..."));
		pictureService.init(() -> logger.info("Finished initial scan of Pictures..."));
		comicService.init(() -> logger.info("Finished initial scan of Comic Books..."));
	}
}
