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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.util.Strings;

/** Class which creates an FFMPEG concatenation task; concatenates multiple video files into one */
@EqualsAndHashCode(callSuper = true)
@Data
public final class FFmpegConcatStreamTask extends FFmpegStreamTask {

  private static final String CONCAT_FILENAME = "concat.txt";
  private final List<URI> uris;
  private Path concatFile;

  @lombok.Builder
  public FFmpegConcatStreamTask(
      String command,
      Path playlistPath,
      Path dataDir,
      boolean loggingEnabled,
      List<String> transcodeArgs,
      List<URI> uris) {

    this.command = command;
    this.playlistPath = playlistPath;
    this.dataDir = dataDir;
    this.loggingEnabled = loggingEnabled;
    this.transcodeArgs = transcodeArgs;
    this.uris = uris;
  }

  @Override
  protected String getExecCommand() {

    // Collate program arguments, format & return
    final String inputs = getInputString();
    final String arguments = Strings.join(transcodeArgs, ' ');
    return String.format(
        "%s %s %s \"%s\"", this.getCommand(), inputs, arguments, this.getPlaylistPath());
  }

  @Override
  protected void prepareStream() throws IOException {
    // Create output directory
    Files.createDirectories(this.getDataDir());
    // Create URI list text file
    this.concatFile = createConcatFile();
  }

  @Override
  protected String getInputString() {
    return String.format("-f concat -safe 0 -i %s", this.concatFile);
  }

  /**
   * Create the text file (concat.txt) used by FFMPEG for concatenation
   *
   * @return The path of the concat.txt file
   * @throws IOException If there is an error creating or writing the file
   */
  private Path createConcatFile() throws IOException {

    // Map each URI to en entry in the concat file
    final String concatFileText =
        uris.stream().map(url -> String.format("file '%s'\n", url)).collect(Collectors.joining());
    // Write data to file
    final Path concatFilePath = Path.of(getDataDir().toAbsolutePath().toString(), CONCAT_FILENAME);
    return Files.writeString(concatFilePath, concatFileText);
  }
}
