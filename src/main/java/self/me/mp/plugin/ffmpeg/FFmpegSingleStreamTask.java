package self.me.mp.plugin.ffmpeg;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Data
@EqualsAndHashCode(callSuper = true)
public class FFmpegSingleStreamTask extends FFmpegStreamTask {

	private static final DateTimeFormatter LOGFILE_TIMESTAMP_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss");

	private final boolean loggingEnabled;

	@Builder
	public FFmpegSingleStreamTask(
			String execCommand,
			TranscodeRequest request,
			boolean loggingEnabled) {
		this.execCommand = execCommand;
		this.request = request;
		this.loggingEnabled = loggingEnabled;
	}

	@Override
	protected List<String> createExecCommand() {
		// Collate program arguments, format & return
		final List<String> arguments = new ArrayList<>();
		arguments.add(getExecCommand());
		arguments.addAll(request.getBaseArgs());
		arguments.addAll(getInputArgs().toList());
		arguments.addAll(getTranscodeArgs());
		arguments.add(request.getTo().toString());
		return arguments.stream()
				.filter(Objects::nonNull)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	/**
	 * Create directory to hold all streaming data
	 *
	 * @throws IOException If there are any problems with stream preparation
	 */
	@Override
	protected void prepareStream() throws IOException {
		// Create output directory
		Files.createDirectories(request.getTo().getParent());
	}

	@Override
	protected Stream<String> getInputArgs() {
		return request.getFrom()
				.stream()
				.flatMap(i -> Stream.of("-i", Utilities.getNormalizedPath(i)));
	}

	private @NotNull List<String> getTranscodeArgs() {
		List<String> args = new ArrayList<>();
		args.add("-c:v");
		args.add(request.getVideoCodec());
		args.add("-c:a");
		args.add(request.getAudioCodec());
		Map<String, Object> additionalArgs = request.getAdditionalArgs();
		if (additionalArgs != null) {
			additionalArgs.forEach((key, value) -> {
				args.add(key);
				args.add(value.toString());
			});
		}
		// todo - map streams
		return args;
	}
}
