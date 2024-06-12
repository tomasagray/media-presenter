package net.tomasbot.mp.unit.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import net.tomasbot.mp.api.service.RecursiveWatcherService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("RecursiveWatcherService testing & validation")
class RecursiveWatcherServiceTest {

  private static final Logger logger = LogManager.getLogger(RecursiveWatcherServiceTest.class);
  private static final List<Path> cleanupFiles = new ArrayList<>();
  private final RecursiveWatcherService watcherService;

  @Value("#{environment.DATA_ROOT}")
  private Path watchDir;

  @Autowired
  RecursiveWatcherServiceTest(RecursiveWatcherService watcherService) {
    this.watcherService = watcherService;
  }

  @AfterAll
  static void cleanup() throws IOException {
    for (Path path : cleanupFiles) {
      File file = path.toFile();
      logger.info("Deleting test file: {}", file);
      boolean deleted = file.delete();
      if (!deleted || file.exists()) {
        throw new IOException("Could not delete test file: " + file);
      }
    }
  }

  @BeforeEach
  void setup() throws IOException {
    createDirectory(watchDir);
    createDirectory(watchDir.resolve("videos"));
    createDirectory(watchDir.resolve("pics"));
    createDirectory(watchDir.resolve("comics"));
  }

  private void createDirectory(@NotNull Path dir) throws IOException {
    logger.info("Setting up test directory: {}", dir);
    File file = dir.toFile();
    if (!file.exists()) {
      logger.info("Making test directory: {}", dir);
      boolean made = file.mkdirs();
      if (!made) throw new IOException("Could not create test directory: " + dir);
    }
  }

  @Test
  @DisplayName("Verify service can watch a specified directory")
  void watch() throws IOException {
    logger.info("Watching directory: {}", watchDir);
    watcherService.watch(
        watchDir,
        file -> logger.info("Found file: {}", file),
        (path, kind) -> logger.info("Watched event {} occurred at: {}", kind, path));
    List<Path> roots = watcherService.getWatchRoots();
    logger.info("WatchRoots are now: {}", roots);
    assertFalse(roots.isEmpty());
  }

  private @NotNull Path createTestFile(@NotNull Path location) throws IOException {

    Path testFile = Path.of("test.txt");
    Path resolved = location.resolve(testFile);
    File createFile = resolved.toFile();
    try (final PrintWriter writer = new PrintWriter(new FileWriter(createFile))) {
      String timestamp = String.format("This test file was created: %s%n", LocalDateTime.now());
      writer.write(timestamp);
      writer.write("=-".repeat(100) + "\n");
    }
    assertTrue(createFile.exists());
    cleanupFiles.add(resolved);
    return resolved;
  }

  @Test
  @DisplayName("Ensure ignored paths are actually ignored")
  void ignore() throws IOException {
    // given
    assert watchDir != null;
    File[] subDirs = watchDir.toFile().listFiles();
    assert subDirs != null && subDirs.length >= 1;
    Path ignoreDir = subDirs[0].toPath();
    logger.info("Ignoring: {}", ignoreDir);

    // when
    watcherService.watch(
        watchDir,
        (path, kind) -> {
          // then
          logger.error("Ignored directory: {} triggered WatchEvent: {}", path, kind);
          throw new RuntimeException("This code should not have executed!");
        });
    watcherService.ignore(ignoreDir);

    Path testFile = createTestFile(ignoreDir);
    logger.info("Created test file in ignored directory: {}", testFile);

    // then we should reach here
    assert true;

    // reset for other tests
    watcherService.unwatch(watchDir);
    watcherService.unignore(ignoreDir);
  }
}
