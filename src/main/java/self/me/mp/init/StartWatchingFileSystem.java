package self.me.mp.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import self.me.mp.api.service.RecursiveWatcherService;

@Component
@Order(1)
public class StartWatchingFileSystem implements CommandLineRunner {

	private static final Logger logger = LogManager.getLogger(StartWatchingFileSystem.class);

	private final RecursiveWatcherService watcherService;

	public StartWatchingFileSystem(RecursiveWatcherService watcherService) {
		this.watcherService = watcherService;
	}

	@Override
	public void run(String... args) {
		logger.info("Starting filesystem WatchService...");
		watcherService.doWatch();
		logger.info("Filesystem WatchService successfully started.");
	}
}
