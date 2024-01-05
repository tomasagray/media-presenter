package self.me.mp.plugin.ffmpeg;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentSkipListMap;

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
	 * @param uris         URI pointers to video data
	 * @param playlistPath The output location for stream data
	 * @return The path of the playlist file produced by FFMPEG
	 */
	public LoggableThread streamUris(@NotNull final Path playlistPath, @NotNull final URI... uris) {

		// Get absolute path for task key
		final Path absolutePath = playlistPath.toAbsolutePath();
		checkTaskAlreadyExecuting(absolutePath);
		// Create the streaming task
		// todo - make a real request
		SimpleTranscodeRequest request = SimpleTranscodeRequest.builder().build();
		final FFmpegStreamTask streamTask = ffmpeg.getHlsStreamTask(request);
		// Add to collection
		streamingTasks.put(absolutePath, streamTask);
		// Return playlist file path
		return streamTask;
	}

	public LoggableThread transcode(
			@NotNull URI from,
			@NotNull Path to,
			@NotNull String videoCodec,
			@NotNull String audioCodec) {
		SimpleTranscodeRequest request = SimpleTranscodeRequest.builder()
				.from(from)
				.to(to)
				.videoCodec(videoCodec)
				.audioCodec(audioCodec)
				.build();
		return ffmpeg.getTranscodeTask(request);
	}

	public LoggableThread transcode(@NotNull TranscodeRequest request) {
		return ffmpeg.getTranscodeTask(request);
	}

	/**
	 * Cancels all streaming tasks running in the background
	 */
	public void interruptAllStreamTasks() {
		ProcessHandle.allProcesses().filter(p -> p.info().command().map(c -> c.contains("ffmpeg")).orElse(false)).forEach(ProcessHandle::destroyForcibly);
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

	public String getVersion() throws IOException {
		return ffmpeg.getVersion();
	}

	public Path createThumbnail(@NotNull Path video, @NotNull Path thumb, @NotNull LocalTime time, int w, int h) throws IOException {
		return ffmpeg.createThumbnail(video, thumb, time, w, h);
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
				throw new IllegalThreadStateException("FFmpeg has already started streaming to path: " + absolutePath);
			}
		}
	}
}
