package net.tomasbot.mp.api.service;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import net.tomasbot.mp.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RecursiveWatcherService {

  private static final Logger logger = LogManager.getLogger(RecursiveWatcherService.class);
  @Getter private final List<Path> watchRoots = new ArrayList<>();
  private final List<Path> ignorePaths = new ArrayList<>();
  private final Map<WatchKey, WatchHandler> watches = new HashMap<>();
  private final WatchService watchService;

  public RecursiveWatcherService() throws IOException {
    this.watchService = FileSystems.getDefault().newWatchService();
  }

  public void watch(@NotNull Path path, @NotNull BiConsumer<Path, WatchEvent.Kind<?>> eventHandler)
      throws IOException {
    watch(path, null, eventHandler);
  }

  public void watch(
      @NotNull Path path,
      @Nullable Consumer<Path> onScanFile,
      @NotNull BiConsumer<Path, WatchEvent.Kind<?>> eventHandler)
      throws IOException {
    watch(path, onScanFile, eventHandler, null);
  }

  public void watch(
      @NotNull Path path,
      @Nullable Consumer<Path> onScanFile,
      @NotNull BiConsumer<Path, WatchEvent.Kind<?>> eventHandler,
      @Nullable Procedure onFinish)
      throws IOException {

    File file = path.toFile();
    if (!(file.exists()) || !(file.isDirectory())) {
      throw new IOException("Cannot watch non-existent directory: " + path);
    }
    watchRoots.add(path);
    walkTreeAndSetWatches(path, onScanFile, eventHandler, onFinish);
  }

  public synchronized void walkTreeAndSetWatches(
      @NotNull Path path,
      @Nullable Consumer<Path> onScanFile,
      @NotNull BiConsumer<Path, WatchEvent.Kind<?>> handler,
      @Nullable Procedure onFinish) {
    try {
      logger.trace("Now watching recursively: {}", path);
      Files.walkFileTree(
          path,
          new FileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(
                Path dir, @NotNull BasicFileAttributes attrs) {
              if (ignorePaths.contains(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
              } else {
                registerWatch(dir, handler);
                return FileVisitResult.CONTINUE;
              }
            }

            @Override
            public @NotNull FileVisitResult visitFile(
                Path file, @NotNull BasicFileAttributes attrs) {
              if (onScanFile != null) {
                onScanFile.accept(file);
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) {
              logger.error("Visiting file {} failed: {}", file, exc.getMessage(), exc);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult postVisitDirectory(Path dir, IOException exc) {
              return FileVisitResult.CONTINUE;
            }
          });
      logger.info("Finished scan of: {}", path);
    } catch (IOException e) {
      logger.error("Error walking directory: {}; {}", path, e.getMessage(), e);
    } finally {
      if (onFinish != null) {
        onFinish.run();
      }
    }
  }

  public void unwatch(@NotNull Path path) {
    watchRoots.remove(path);
    walkTreeAndUnsetWatches(path);
  }

  private synchronized void walkTreeAndUnsetWatches(@NotNull Path path) {
    try {
      Files.walkFileTree(
          path,
          new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(
                Path path, @NotNull BasicFileAttributes attr) {
              unregisterWatch(path);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException ignore) {
    }
  }

  private synchronized void registerWatch(Path path, BiConsumer<Path, WatchEvent.Kind<?>> handler) {
    logger.trace("Now watching: {}", path);
    Optional<Map.Entry<WatchKey, WatchHandler>> watchOptional = getWatchForPath(path);
    if (watchOptional.isEmpty()) {
      try {
        WatchEvent.Kind<?>[] kinds = {ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};
        WatchKey watchKey = path.register(watchService, kinds);
        watches.put(watchKey, new WatchHandler(path, handler));
      } catch (IOException e) {
        logger.error("Could not register watch on: {}", path, e);
      }
    }
  }

  public void ignore(@NotNull Path path) throws IOException {
    if (!Files.exists(path)) {
      throw new IOException("Path does not exist: " + path);
    }
    ignorePaths.add(path);
    unregisterStaleWatches();
  }

  public void unignore(@NotNull Path path) {
    ignorePaths.remove(path);
  }

  @Async("watcher")
  public void doWatch() {
    try {
      WatchKey key;
      while ((key = watchService.take()) != null) {
        WatchHandler handler = watches.get(key);
        if (handler == null) {
          logger.warn("WatchHandler missing for WatchKey: {}; removing", key);
          watches.remove(key);
          continue;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          if (kind.equals(ENTRY_DELETE)) {
            unregisterStaleWatches();
          }
          Path context = (Path) event.context();
          Path resolved = handler.path().resolve(context);
          handler.handler().accept(resolved, kind);
        }
        key.reset();
      }
    } catch (InterruptedException ignore) {
      logger.info("{} was interrupted", this.getClass().getSimpleName());
    } finally {
      logger.info("{} has been shutdown.", this.getClass().getSimpleName());
    }
  }

  public synchronized void unregisterStaleWatches() {
    List<Path> stalePaths =
        watches.values().stream().map(WatchHandler::path).filter(this::isStale).toList();
    stalePaths.forEach(this::unregisterWatch);
  }

  private boolean isStale(Path path) {
    return !Files.exists(path, LinkOption.NOFOLLOW_LINKS) || ignorePaths.contains(path);
  }

  private synchronized void unregisterWatch(Path dir) {
    getWatchForPath(dir)
        .ifPresent(
            entry -> {
              WatchKey key = entry.getKey();
              key.cancel();
              watches.remove(key);
              Path path = entry.getValue().path();
              logger.info("Stopped watching directory: {}", path);
            });
  }

  @NotNull
  private Optional<Map.Entry<WatchKey, WatchHandler>> getWatchForPath(Path dir) {
    return watches.entrySet().stream()
        .filter(entry -> entry.getValue().path().equals(dir))
        .findFirst();
  }

  private record WatchHandler(Path path, BiConsumer<Path, WatchEvent.Kind<?>> handler) {}
}
