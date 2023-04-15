/*
 * Copyright (c) 2020.
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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.util.Strings;

@EqualsAndHashCode(callSuper = true)
@Data
public class FFmpegSingleStreamTask extends FFmpegStreamTask {

  private static final DateTimeFormatter LOGFILE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");

  private final URI uri;

  @lombok.Builder
  public FFmpegSingleStreamTask(
      String command,
      Path playlistPath,
      Path dataDir,
      boolean loggingEnabled,
      List<String> transcodeArgs,
      URI uri) {

    this.command = command;
    this.playlistPath = playlistPath;
    this.dataDir = dataDir;
    this.loggingEnabled = loggingEnabled;
    this.transcodeArgs = transcodeArgs;
    this.uri = uri;
  }

  @Override
  protected String getExecCommand() {

    // Collate program arguments, format & return
    final String inputs = getInputString();
    final String arguments = Strings.join(transcodeArgs, ' ');
    return String.format(
        "%s %s %s \"%s\"", this.getCommand(), inputs, arguments, this.getPlaylistPath());
  }

  /**
   * Create directory to hold all streaming data
   *
   * @throws IOException If there are any problems with stream preparation
   */
  @Override
  protected void prepareStream() throws IOException {
    // Create output directory
    Files.createDirectories(this.getDataDir());
  }

  @Override
  protected String getInputString() {
    return String.format("-i %s", uri);
  }
}