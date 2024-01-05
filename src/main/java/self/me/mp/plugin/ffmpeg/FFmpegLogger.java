package self.me.mp.plugin.ffmpeg;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class FFmpegLogger implements ThreadLogger {

	@Override
	@NotNull
	public synchronized Flux<String> beginLogging(
			@NotNull Process process, @NotNull AsynchronousFileChannel fileChannel) {
		final AtomicInteger pos = new AtomicInteger(0);
		return Flux.using(
						() ->
								new BufferedReader(
										new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
										.lines(),
						Flux::fromStream,
						Stream::close)
				.doOnNext(
						data -> {
							// write to log file
							final String line = data + "\n";
							final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
							fileChannel.write(ByteBuffer.wrap(bytes), pos.getAndAdd(bytes.length));
						});
	}
}
