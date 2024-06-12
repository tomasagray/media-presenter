package net.tomasbot.mp.plugin.ffmpeg;

import java.nio.channels.AsynchronousFileChannel;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

public interface ThreadLogger {

	Flux<String> beginLogging(@NotNull Process process, @NotNull AsynchronousFileChannel fileChannel);

}
