package self.me.mp.plugin.ffmpeg;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpeg {

	private static final String SEGMENT_PATTERN = "segment_%05d.ts";

	private final List<String> baseArgs;


	@Getter
	@Setter
	boolean loggingEnabled = true;

	public FFmpeg(@NotNull final String execPath) {
		this.baseArgs =
				List.of(
						execPath,
						"-v info",
						"-y",
						"-protocol_whitelist concat,file,http,https,tcp,tls,crypto");
	}

	public String getVersion() throws IOException {
		final Pattern versionPattern = Pattern.compile("ffmpeg version ([\\w.-]+)");
		final String cmd = String.join(" ", this.baseArgs);
		final Process process = Runtime.getRuntime().exec(cmd);

		try (InputStreamReader in = new InputStreamReader(process.getErrorStream());
		     BufferedReader reader = new BufferedReader(in)) {
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher matcher = versionPattern.matcher(line);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
		}
		throw new IOException("Could not determine FFMPEG version");
	}

	/**
	 * Create a concatenated FFMPEG stream of the given URIs at the given storage location
	 *
	 * @param uris         A List of file resource pointers
	 * @param playlistPath The location on disk to store stream data
	 * @return A thread task
	 */
	public FFmpegStreamTask getHlsStreamTask(
			@NotNull final Path playlistPath, @NotNull final URI... uris) {

		// Assemble arguments
		final String command = Strings.join(baseArgs, ' ');
		final Path playlistAbsolutePath = playlistPath.toAbsolutePath();
		final Path dataDir = playlistAbsolutePath.getParent();
		final List<String> transcodeArgs = getDefaultTranscodeArgs(dataDir);

		if (uris.length > 1) {
			// Create FFMPEG CLI command & return
			return FFmpegConcatStreamTask.builder()
					.command(command)
					.uris(List.of(uris))
					.transcodeArgs(transcodeArgs)
					.playlistPath(playlistAbsolutePath)
					.dataDir(dataDir)
					.loggingEnabled(loggingEnabled)
					.build();
		} else {
			URI uri = uris[0];
			// Create streaming task & return
			return FFmpegSingleStreamTask.builder()
					.command(command)
					.uri(uri)
					.transcodeArgs(transcodeArgs)
					.playlistPath(playlistAbsolutePath)
					.dataDir(dataDir)
					.loggingEnabled(loggingEnabled)
					.build();
		}
	}

	private List<String> getDefaultTranscodeArgs(@NotNull final Path storageLocation) {

		final List<String> transcodeArgs = new ArrayList<>();
		final Path absoluteStorageLocation = storageLocation.toAbsolutePath();
		final Path segmentPattern = absoluteStorageLocation.resolve(SEGMENT_PATTERN);
		// Add arguments
		transcodeArgs.add("-vcodec copy");
		transcodeArgs.add("-acodec copy");
		transcodeArgs.add("-muxdelay 0");
		transcodeArgs.add("-f hls");
		transcodeArgs.add("-hls_playlist_type event");
		transcodeArgs.add("-hls_segment_filename");
		// Add segment output pattern
		final String segments = String.format("\"%s\"", segmentPattern);
		transcodeArgs.add(segments);
		return transcodeArgs;
	}

	public Path createThumbnail(
			@NotNull Path video,
			@NotNull Path thumb,
			@NotNull LocalTime time,
			int w, int h) throws IOException {

		ArrayList<String> args = marshallThumbArgs(video, thumb, time, w, h);
		Process process = Runtime.getRuntime().exec(Strings.join(args, ' '));
		try (InputStreamReader in = new InputStreamReader(process.getErrorStream());
		     BufferedReader reader = new BufferedReader(in)
		) {
			reader.lines().forEach(System.out::println);
		}
		return thumb;
	}

	@NotNull
	private ArrayList<String> marshallThumbArgs(
			@NotNull Path video, @NotNull Path thumb, @NotNull LocalTime time, int w, int h) {

		ArrayList<String> args = new ArrayList<>(baseArgs);
		args.add("-loglevel error");
		args.add("-ss " + time);
		args.add("-i " + video);
		args.add(String.format("-vf scale=%d:%d", w, h));
		args.add("-vframes 1");
		args.add(thumb.toString());
		return args;
	}
}
