package net.tomasbot.mp.api.service;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public interface FileScanningService {

  void scanFile(@NotNull Path file, @NotNull Collection<Path> existing);

  void handleFileEvent(@NotNull Path file, @NotNull WatchEvent.Kind<?> kind);
}
