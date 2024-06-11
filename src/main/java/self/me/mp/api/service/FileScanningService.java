package self.me.mp.api.service;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.MultiValueMap;

public interface FileScanningService {

  void scanFile(@NotNull Path file, @NotNull Collection<Path> existing);

  void handleFileEvent(@NotNull Path file, @NotNull WatchEvent.Kind<?> kind);

  void saveScannedData();

  MultiValueMap<String, Path> getInvalidFiles();
}
