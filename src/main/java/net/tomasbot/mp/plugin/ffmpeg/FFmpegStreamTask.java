package net.tomasbot.mp.plugin.ffmpeg;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class FFmpegStreamTask extends LoggableThread {

  private static final Logger logger = LogManager.getLogger(FFmpegStreamTask.class);
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");
  private static final String LOG_FILENAME = "ffmpeg-%s.log";

  protected String execCommand;
  protected TranscodeRequest request;

  @Override
  public void start() {
    try {
      prepareStream();
      final List<String> command = createExecCommand();
      logger.debug("Beginning transcode with command:\n\t{}", Strings.join(command, ' '));

      this.process = new ProcessBuilder().command(command).start();
      if (loggingEnabled) {
        final Path logFile = getLogFile();
        final OpenOption[] options = {StandardOpenOption.CREATE, StandardOpenOption.WRITE};
        final AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(logFile, options);
        logPublisher =
            new FFmpegLogger()
                .beginLogging(this.process, fileChannel)
                .doOnNext(this.onEvent)
                .doOnError(e -> this.onError.accept(e))
                .doOnComplete(
                    () -> process.onExit().thenAccept(p1 -> onComplete.accept(p1.exitValue())));
        logPublisher.subscribe();
      }
    } catch (Throwable e) {
      logger.error(e.getMessage(), e);
    }
  }

  @NotNull
  private Path getLogFile() {
    final Path loggingDir = request.getTo().getParent();
    final String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    final String filename = String.format(LOG_FILENAME, timestamp);
    return loggingDir.resolve(filename);
  }

  /**
   * Forcefully halt execution of this task
   *
   * @return True/false if the task was successfully killed
   */
  public final boolean kill() {
    // Ensure process exists
    if (process != null) {
      ProcessHandle.allProcesses()
          .filter(p -> p.pid() == process.pid())
          .findFirst()
          .ifPresent(ProcessHandle::destroyForcibly);
      // Ensure process is dead
      return !process.isAlive();
    }
    return false;
  }

  /**
   * Get a formatted executable command (system CLI)
   *
   * @return The execution command
   */
  abstract List<String> createExecCommand();

  /**
   * Perform necessary preliminary setup tasks
   *
   * @throws IOException If there are any problems with stream preparation
   */
  abstract void prepareStream() throws IOException;

  /**
   * Returns a formatted String containing the input portion of the FFMPEG command
   *
   * @return The input portion of the FFMPEG command
   */
  abstract Stream<String> getInputArgs();
}
