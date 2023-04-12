package self.me.mp.plugin.ffmpeg;

import org.jetbrains.annotations.NotNull;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;
import self.me.mp.util.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FFprobe {

	private final List<String> baseArgs;

	public FFprobe(@NotNull final String execPath) {

		// Setup global CLI arguments
		baseArgs =
				List.of(
						execPath,
						"-hide_banner",
						"-print_format json",
						"-show_streams",
						"-show_format",
						"-show_chapters");
	}

	/**
	 * Retrieve metadata from an audio/video file
	 *
	 * @param uri The file resource pointer
	 * @return The file metadata
	 * @throws IOException If the metadata could not be read or parsed
	 */
	public FFmpegMetadata getFileMetadata(@NotNull final URI uri) throws IOException {
		return JsonParser.fromJson(readFileMetadata(uri), FFmpegMetadata.class);
	}

	/**
	 * Read audio/video file metadata from a URI
	 *
	 * @param uri The URI of the video file
	 * @throws IOException If there is an error reading data
	 */
	private String readFileMetadata(@NotNull final URI uri) throws IOException {

		// Assemble args for this job
		List<String> processArgs = new ArrayList<>(baseArgs);
		// Add remote URL to job args
		processArgs.add(uri.toString());
		// Create process for job
		final String cmd = String.join(" ", processArgs);
		Process process = Runtime.getRuntime().exec(cmd);
		// Fetch remote data
		try (InputStream is = process.getInputStream();
		     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			// Read data and collect as a String
			return reader.lines().collect(Collectors.joining(""));
		} finally {
			// Ensure process closed
			process.destroy();
		}
	}
}
