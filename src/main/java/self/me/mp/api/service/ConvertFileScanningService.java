package self.me.mp.api.service;

import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public interface ConvertFileScanningService<T> extends FileScanningService {

	void scanAddFile(@NotNull Path file);

}
