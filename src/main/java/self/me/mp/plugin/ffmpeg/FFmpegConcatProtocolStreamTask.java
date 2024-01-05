package self.me.mp.plugin.ffmpeg;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@EqualsAndHashCode(callSuper = true)
public final class FFmpegConcatProtocolStreamTask extends FFmpegStreamTask {

	private final Path dataDir;

	@Builder
	public FFmpegConcatProtocolStreamTask(
			@NotNull String command,
			@NotNull TranscodeRequest request,
			boolean loggingEnabled) {
		this.execCommand = command;
		if (Files.isDirectory(request.getTo())) {
			this.dataDir = request.getTo().getParent();
		} else {
			this.dataDir = request.getTo();
		}
		this.loggingEnabled = loggingEnabled;
	}

	@Override
	@Unmodifiable
	@NotNull
	protected List<String> createExecCommand() {
		// Collate program arguments, format & return
		final List<String> command = new ArrayList<>();
		command.add(this.getExecCommand());
		command.addAll(getInputArgs().toList());
		command.addAll(getArgumentList(request.getAdditionalArgs()));
		command.add(request.getTo().toString());
		return command;
	}

	private List<String> getArgumentList(@NotNull Map<String, Object> args) {
		return args.entrySet().stream()
				.flatMap(entry -> Stream.of(entry.getKey(), entry.getValue().toString()))
				.toList();
	}

	@Override
	void prepareStream() throws IOException {
		// Create output directory
		Files.createDirectories(this.getDataDir());
	}

	@Override
	protected @NotNull Stream<String> getInputArgs() {
		// Concatenate URIs
		final String concatText = request.getFrom()
				.stream()
				.map(URI::toString)
				.collect(Collectors.joining("|"));
		final String inputs = String.format("\"concat:%s\"", concatText);
		return Stream.of("-i", inputs);
	}
}
