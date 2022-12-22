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
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentSkipListMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

@Component
public class FFmpegPlugin {

  private final FFmpegPluginProperties pluginProperties;
  private final FFmpeg ffmpeg;
  private final FFprobe ffprobe;
  private final ConcurrentSkipListMap<Path, FFmpegStreamTask> streamingTasks =
      new ConcurrentSkipListMap<>();

  // TODO: make this its own JAR
  public FFmpegPlugin(@NotNull final FFmpegPluginProperties pluginProperties) {

    this.pluginProperties = pluginProperties;
    // Create executable instances
    this.ffmpeg = new FFmpeg(pluginProperties.getFfmpegLocation());
    this.ffprobe = new FFprobe(pluginProperties.getFfprobeLocation());
  }

  /**
   * Create an HLS stream from a given collection of URIs
   *
   * @param uris URI pointers to video data
   * @param playlistPath The output location for stream data
   * @return The path of the playlist file produced by FFMPEG
   */
  public FFmpegStreamTask streamUris(@NotNull final Path playlistPath, @NotNull final URI... uris) {

    // Get absolute path for task key
    final Path absolutePath = playlistPath.toAbsolutePath();
    checkTaskAlreadyExecuting(absolutePath);
    // Create the streaming task
    final FFmpegStreamTask streamTask = ffmpeg.getHlsStreamTask(playlistPath, uris);
    // Add to collection
    streamingTasks.put(absolutePath, streamTask);
    // Return playlist file path
    return streamTask;
  }

  /** Cancels all streaming tasks running in the background */
  public void interruptAllStreamTasks() {
    ProcessHandle.allProcesses()
        .filter(p -> p.info().command().map(c -> c.contains("ffmpeg")).orElse(false))
        .forEach(ProcessHandle::destroyForcibly);
    streamingTasks.clear();
  }

  /**
   * Kills a task associated with the given directory, if there is one
   *
   * @param outputPath The path of the stream data
   */
  public boolean interruptStreamingTask(@NotNull final Path outputPath) {

    // Get absolute path for task key
    final Path absolutePath = outputPath.toAbsolutePath();
    // Get requested task
    final FFmpegStreamTask streamingTask = streamingTasks.get(absolutePath);
    boolean killed = false;
    if (streamingTask != null) {
      // kill task
      killed = streamingTask.kill();
    }
    streamingTasks.remove(absolutePath);
    return killed;
  }

  /**
   * Returns the number of currently executing streaming tasks
   *
   * @return Number of streaming tasks
   */
  public int getStreamingTaskCount() {
    return streamingTasks.size();
  }

  /**
   * Wrap the FFprobe metadata method
   *
   * @param uri The URI of the audio/video file
   * @return An FFmpegMetadata object of the file's metadata, or null
   * @throws IOException I/O problem
   */
  public FFmpegMetadata readFileMetadata(@NotNull final URI uri) throws IOException {
    return ffprobe.getFileMetadata(uri);
  }

  public String getTitle() {
    return pluginProperties.getTitle();
  }

  public String getDescription() {
    return pluginProperties.getDescription();
  }

  /**
   * Determines if there is a task streaming to the given directory
   *
   * @param absolutePath The path of the streaming task
   */
  private void checkTaskAlreadyExecuting(@NotNull final Path absolutePath) {

    // Check if a task is already working in path
    FFmpegStreamTask prevTask = streamingTasks.get(absolutePath);
    if (prevTask != null) {
      if (!prevTask.isAlive()) {
        // Kill zombie task & proceed
        prevTask.kill();
        streamingTasks.remove(absolutePath);
      } else {
        throw new IllegalThreadStateException(
            "FFmpeg has already started streaming to path: " + absolutePath);
      }
    }
  }
}
