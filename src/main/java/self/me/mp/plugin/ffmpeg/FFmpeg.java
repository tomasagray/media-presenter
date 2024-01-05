package self.me.mp.plugin.ffmpeg;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpeg {

	private static final String SEGMENT_PATTERN = "segment_%05d.ts";
	private static final Pattern VERSION_PATTERN = Pattern.compile("ffmpeg version ([\\w.-]+)");

	private final String execPath;
	private final List<String> baseArgs;

	@Getter
	@Setter
	boolean loggingEnabled = true;

	public FFmpeg(@NotNull final String execPath) {
		this.execPath = execPath;
		this.baseArgs =
				List.of(
						"-v", "info",
						"-y",
						"-protocol_whitelist", "concat,file,http,https,tcp,tls,crypto");
	}

	public String getVersion() throws IOException {
		final List<String> args = new ArrayList<>(this.baseArgs);
		args.add(0, this.execPath);
		final String cmd = String.join(" ", args);
		final Process process = Runtime.getRuntime().exec(cmd);
		try (InputStreamReader in = new InputStreamReader(process.getErrorStream());
		     BufferedReader reader = new BufferedReader(in)) {
			String line;
			while ((line = reader.readLine()) != null) {
				Matcher matcher = VERSION_PATTERN.matcher(line);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
		}
		throw new IOException("Could not determine FFMPEG version");
	}

	public FFmpegStreamTask getHlsStreamTask(
			@NotNull TranscodeRequest request) {
		request.setAdditionalArgs(getHlsArgs(request.getTo()));
		if (request.getFrom().size() > 1) {
			// Create FFMPEG CLI command & return
			return FFmpegConcatStreamTask.builder()
					.command(execPath)
					.loggingEnabled(loggingEnabled)
					.build();
		} else {
			// Create streaming task & return
			return FFmpegSingleStreamTask.builder()
					.execCommand(execPath)
					.request(request)
					.loggingEnabled(true)
					.build();
		}
	}

	public FFmpegStreamTask getTranscodeTask(@NotNull TranscodeRequest request) {
		ArrayList<String> args = new ArrayList<>(baseArgs);
		request.setBaseArgs(args);
		return FFmpegSingleStreamTask.builder()
				.execCommand(execPath)
				.request(request)
				.build();
	}

	private @NotNull Map<String, Object> getHlsArgs(@NotNull final Path storageLocation) {
		final Map<String, Object> transcodeArgs = new HashMap<>();
		final Path absoluteStorageLocation = Files.isDirectory(storageLocation) ?
				storageLocation.toAbsolutePath() :
				storageLocation.toAbsolutePath().getParent();
		final Path segmentPattern = absoluteStorageLocation.resolve(SEGMENT_PATTERN);
		// Add arguments
		transcodeArgs.put("-vcodec", "copy");
		transcodeArgs.put("-acodec", "copy");
		transcodeArgs.put("-muxdelay", "0");
		transcodeArgs.put("-f", "hls");
		transcodeArgs.put("-hls_playlist_type", "event");
		final String segments = String.format("\"%s\"", segmentPattern);
		transcodeArgs.put("-hls_segment_filename", segments);
		// Add segment output pattern
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
		final ArrayList<String> args = new ArrayList<>(baseArgs);
		args.add(0, this.execPath);
		args.add("-loglevel");
		args.add("error");
		args.add("-ss");
		args.add(time.toString());
		args.add("-i");
		args.add(video.toString());
		args.add("-vf");
		args.add(String.format("scale=%d:%d", w, h));
		args.add("-vframes 1");
		args.add(thumb.toString());
		return args;
	}
}
