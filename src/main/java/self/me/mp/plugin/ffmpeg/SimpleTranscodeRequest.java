package self.me.mp.plugin.ffmpeg;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegStream;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = true)
public class SimpleTranscodeRequest extends TranscodeRequest {

	@Builder
	public SimpleTranscodeRequest(
			URI from,
			Path to,
			String videoCodec,
			String audioCodec,
			List<FFmpegStream> streams,
			Map<String, Object> additionalArgs) {
		this.from = List.of(from);
		this.to = to;
		this.videoCodec = videoCodec;
		this.audioCodec = audioCodec;
		this.streams = streams;
		this.additionalArgs = additionalArgs == null ? null : new HashMap<>(additionalArgs);
	}
}
