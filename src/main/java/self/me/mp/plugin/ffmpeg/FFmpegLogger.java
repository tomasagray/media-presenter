package self.me.mp.plugin.ffmpeg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

public class FFmpegLogger implements ThreadLogger {

  private static void writeLogLine(
      @NotNull AsynchronousFileChannel fileChannel,
      @NotNull String data,
      @NotNull AtomicInteger writePos) {
    final String line = data + "\n";
    final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
    fileChannel.write(ByteBuffer.wrap(bytes), writePos.getAndAdd(bytes.length));
  }

  @Override
  @NotNull
  public synchronized Flux<String> beginLogging(
      @NotNull Process process, @NotNull AsynchronousFileChannel fileChannel) {
    final AtomicInteger writePos = new AtomicInteger(0);
    final InputStream dataStream = process.getErrorStream();
    return Flux.using(
            () ->
                new BufferedReader(new InputStreamReader(dataStream, StandardCharsets.UTF_8))
                    .lines(),
            Flux::fromStream,
            Stream::close)
        .doOnNext(data -> writeLogLine(fileChannel, data, writePos));
  }
}
