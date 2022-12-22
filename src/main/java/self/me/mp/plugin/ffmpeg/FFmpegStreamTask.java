/*
 * Copyright (c) 2022.
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

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import reactor.core.publisher.Flux;

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
