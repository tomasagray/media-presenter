package net.tomasbot.mp.plugin.ffmpeg;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.tomasbot.mp.plugin.ffmpeg.metadata.FFmpegStream;
import org.jetbrains.annotations.NotNull;

public class TranscodeRequestParser {

	private static String getStreamArgs(@NotNull TranscodeRequest request) {
		final List<FFmpegStream> streams = request.getStreams();
		if (streams == null) return "";
		final AtomicInteger streamCount = new AtomicInteger(0);
		return streams.stream()
				.map(stream -> getStreamArgument(streamCount, stream))
				.collect(Collectors.joining(" "));
	}

	@NotNull
	private static String getStreamArgument(@NotNull AtomicInteger streamCount, FFmpegStream stream) {
		validateStream(stream);
		final int streamPos = streamCount.getAndIncrement();
		final String type = "video".equals(stream.getCodec_type()) ? "v" : "a";
		final int index = stream.getIndex();
		final String identifier = String.format("%d:%s:%d", streamPos, type, index);
		return "-map " + identifier;
	}

	private static void validateRequest(@NotNull TranscodeRequest request) {
		if (request.getFrom() == null) {
			throw new IllegalArgumentException("No input file");
		}
		if (request.getTo() == null) {
			throw new IllegalArgumentException("No output specified");
		}
		final String videoCodec = request.getVideoCodec();
		final String audioCodec = request.getAudioCodec();
		if (videoCodec == null || audioCodec == null) {
			throw new IllegalArgumentException("Video codec & audio codec must be specified");
		}
	}

	private static void validateStream(@NotNull FFmpegStream stream) {
		if (stream.getCodec_type() == null) {
			throw new IllegalArgumentException("No stream type specified");
		}
	}

	public String parse(@NotNull TranscodeRequest request) {
		validateRequest(request);

		final String inputs = request.getFrom()
				.stream()
				.map(input -> "-i " + input)
				.collect(Collectors.joining(" "));
		final String videoCodec = "-c:v " + request.getVideoCodec();
		final String audioCodec = "-c:a " + request.getAudioCodec();
		final String streamArgs = getStreamArgs(request);
		final String output = request.getTo().toString();
		return Stream.of(inputs, streamArgs, videoCodec, audioCodec, output)
				.filter(s -> !"".equals(s))
				.collect(Collectors.joining(" "));
	}
}
