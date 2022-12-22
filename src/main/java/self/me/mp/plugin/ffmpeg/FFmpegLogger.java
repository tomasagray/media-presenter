/*
 * Copyright (c) 2021.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.mp.plugin.ffmpeg;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

@Data
public class FFmpegLogger {

  private static final DateTimeFormatter LOGFILE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");

  private final Process process;
  private final Path dataDir;
  private Flux<String> logEmitter;

  public FFmpegLogger(@NotNull final Process process, @NotNull final Path dataDir) {
    this.process = process;
    this.dataDir = dataDir;
  }

  public Flux<String> beginLogging(@NotNull final AsynchronousFileChannel fileChannel) {

    final AtomicInteger pos = new AtomicInteger(0);
    logEmitter =
        Flux.using(
            () ->
                new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))
                    .lines(),
            Flux::fromStream,
            Stream::close);
    return logEmitter.doOnNext(
        data -> {
          // write to log file
          final String line = data + "\n";
          final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
          fileChannel.write(ByteBuffer.wrap(bytes), pos.get());
          pos.getAndAdd(bytes.length);
        });
  }

  /**
   * Create a reference to the log file location; inside streaming directory, with the format:
   * ffmpeg-yyyy-MM-dd_hh-mm-ss.log
   *
   * @return The log file location
   */
  public @NotNull Path getLogFile() {
    // Create log file name from timestamp
    final String timestamp = LocalDateTime.now().format(LOGFILE_TIMESTAMP_FORMATTER);
    final String logFilename = String.format("ffmpeg-%s.log", timestamp);
    // Create file reference in working directory
    return dataDir.resolve(logFilename);
  }
}
