package self.me.mp.plugin.ffmpeg;

import lombok.Data;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegStream;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Data
public abstract class TranscodeRequest {

	protected List<String> baseArgs;
	protected List<URI> from;
	protected Path to;
	protected String videoCodec;
	protected String audioCodec;
	protected List<FFmpegStream> streams;
	protected Map<String, Object> additionalArgs;

}
