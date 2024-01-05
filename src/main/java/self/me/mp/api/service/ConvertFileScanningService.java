package self.me.mp.api.service;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface ConvertFileScanningService<T> extends FileScanningService<T> {

	void scanAddFile(@NotNull Path file);

}
