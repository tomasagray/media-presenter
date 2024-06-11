package self.me.mp.plugin.ffmpeg;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.Data;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegStream;

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
