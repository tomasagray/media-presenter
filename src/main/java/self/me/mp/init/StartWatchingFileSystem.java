package self.me.mp.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import self.me.mp.api.service.RecursiveWatcherService;
import self.me.mp.api.service.VideoService;

import java.io.IOException;

@Component
@Order(2)
public class StartWatchingFileSystem implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(StartWatchingFileSystem.class);

	private final RecursiveWatcherService watcherService;
	private final VideoService videoService;

	public StartWatchingFileSystem(
			RecursiveWatcherService watcherService,
			VideoService videoService) {
		this.watcherService = watcherService;
		this.videoService = videoService;
	}

	@Override
	public void run(String... args) throws IOException {
		logger.info("Starting filesystem WatchService...");
		watcherService.doWatch();
		logger.info("Filesystem WatchService started.");

		// perform initial file scans...
		videoService.init();
	}
}
