package self.me.mp.plugin.ffmpeg;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract class FFmpegStreamTask extends Thread {

	private static final DateTimeFormatter LOGFILE_TIMESTAMP_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");

	protected String command;
	protected List<String> transcodeArgs;
  protected Path playlistPath;
  protected Path dataDir;
  protected boolean loggingEnabled;
  protected Process process;
  protected Flux<String> logAdapter;

  public Process execute() throws IOException {
    prepareStream();
    final String command = getExecCommand();
    this.process = new ProcessBuilder().command(command).start();
    return this.process;
  }

  @SneakyThrows
  @Override
  public void run() {
    final Process process = this.execute();
    process.waitFor();
    process.destroy();
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
  abstract String getExecCommand();

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
  abstract String getInputString();
}
