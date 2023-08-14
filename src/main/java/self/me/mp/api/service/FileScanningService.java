package self.me.mp.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.MultiValueMap;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;

public interface FileScanningService<T> {

	void scanFile(
			@NotNull Path file,
			@NotNull Collection<T> existing,
			@NotNull Consumer<T> onSave);

	MultiValueMap<String, Path> getInvalidFiles();
}
