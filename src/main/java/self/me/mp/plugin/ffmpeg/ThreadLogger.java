package self.me.mp.plugin.ffmpeg;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

import java.nio.channels.AsynchronousFileChannel;

public interface ThreadLogger {

	Flux<String> beginLogging(@NotNull Process process, @NotNull AsynchronousFileChannel fileChannel);

}
