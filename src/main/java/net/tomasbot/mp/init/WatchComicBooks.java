package net.tomasbot.mp.init;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import net.tomasbot.mp.api.service.ComicBookService;
import net.tomasbot.mp.api.service.ComicScanningService;
import net.tomasbot.mp.api.service.RecursiveWatcherService;
import net.tomasbot.mp.model.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@Profile("!test-volatile")
public class WatchComicBooks implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(WatchComicBooks.class);

  private final RecursiveWatcherService watcherService;
  private final ComicScanningService scanningService;
  private final ComicBookService comicBookService;

  @Value("${comics.location}")
  private Path comicsLocation;

  public WatchComicBooks(
      RecursiveWatcherService watcherService,
      ComicScanningService scanningService,
      ComicBookService comicBookService) {
    this.watcherService = watcherService;
    this.scanningService = scanningService;
    this.comicBookService = comicBookService;
  }

  private void initializeComicBookLocation() throws IOException {
    File file = comicsLocation.toFile();
    if (!file.exists()) {
      logger.info("Comic Book storage location: {} does not exist; creating...", comicsLocation);
      if (!file.mkdirs()) {
        throw new IOException(
            "Could not create location for Comic Book storage: " + comicsLocation);
      }
    }
  }

  private void finishComicBookScan(Instant jobStart) {
    Duration jobDuration = Duration.between(jobStart, Instant.now());
    logger.info("Initial scan of Comic Book files finished in {}ms", jobDuration.toMillis());
  }

  @Override
  @Async("startup")
  public void run(String... args) throws Exception {
    initializeComicBookLocation();

    Set<Path> existing =
        comicBookService.getAllPages().stream()
            .map(Image::getUri)
            .map(Path::of)
            .collect(Collectors.toSet());
    logger.info("At scan init, there are: {} Comic Book pages in the database...", existing.size());

    logger.info("Scanning Comic Books in: '{}' ...", comicsLocation);
    final Instant jobStart = Instant.now();
    watcherService.watch(
        comicsLocation,
        path -> scanningService.scanFile(path, existing),
        scanningService::handleFileEvent,
        () -> finishComicBookScan(jobStart));
  }
}
