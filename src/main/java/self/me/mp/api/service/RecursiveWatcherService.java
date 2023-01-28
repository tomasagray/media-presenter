package self.me.mp.api.service;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class RecursiveWatcherService {

  private static final Logger logger = LogManager.getLogger(RecursiveWatcherService.class);

  private final List<Path> watchRoots = new ArrayList<>();
  private final List<Path> ignorePaths = new ArrayList<>();
  private final WatchListener listener;
  private final Map<Path, WatchKey> watchPathKeyMap = new HashMap<>();
  //  @Value("${file-system.watcher.settle-delay}")
  private int settleDelay;
  private WatchService watchService;
  private Thread watchThread;
  private Timer timer;

  public RecursiveWatcherService() {
    this.listener = () -> System.out.println("Event occurred.");
  }

  private synchronized void resetWaitSettlementTimer() {
    logger.info("File system events registered. Waiting " + settleDelay + "ms for settlement ....");
    if (timer != null) {
      timer.cancel();
    }
    timer = new Timer("WatchTimer");
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            logger.info("File system actions (on watched folders) settled. Updating watches ...");
            walkTreeAndSetWatches();
            unregisterStaleWatches();
            fireListenerEvents();
          }
        },
        settleDelay);
  }

  private synchronized void walkTreeAndSetWatches() {
    logger.info("Registering new folders at watch service ...");

    try {
      Files.walkFileTree(
          Path.of(""),
          new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
              if (ignorePaths.contains(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
              } else {
                registerWatch(dir);
                return FileVisitResult.CONTINUE;
              }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      // Don't care
    }
  }

  private synchronized void unregisterStaleWatches() {
    Set<Path> paths = new HashSet<>(watchPathKeyMap.keySet());
    Set<Path> stalePaths = new HashSet<>();

    for (Path path : paths) {
      if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        stalePaths.add(path);
      }
    }

    if (stalePaths.size() > 0) {
      logger.info("Cancelling stale path watches ...");

      for (Path stalePath : stalePaths) {
        unregisterWatch(stalePath);
      }
    }
  }

  private synchronized void fireListenerEvents() {
    if (listener != null) {
      logger.info("- Firing watch event (watchEventsOccurred) ...");
      listener.watchEventsOccurred();
    }
  }

  private synchronized void registerWatch(Path dir) {
    if (!watchPathKeyMap.containsKey(dir)) {
      logger.info("- Registering " + dir);

      try {
        WatchKey watchKey =
            dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
        watchPathKeyMap.put(dir, watchKey);
      } catch (IOException e) {
        // Don't care!
      }
    }
  }

  private synchronized void unregisterWatch(Path dir) {
    WatchKey watchKey = watchPathKeyMap.get(dir);

    if (watchKey != null) {
      logger.info("- Cancelling " + dir);

      watchKey.cancel();
      watchPathKeyMap.remove(dir);
    }
  }

  public interface WatchListener {
    void watchEventsOccurred();
  }
}
