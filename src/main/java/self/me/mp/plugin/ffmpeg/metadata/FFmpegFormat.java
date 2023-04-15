package self.me.mp.plugin.ffmpeg.metadata;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class FFmpegFormat {

	private String filename;
	private int nb_streams;
	private int nb_programs;
	private String format_name;
	private String format_long_name;
	private double start_time;
	private double duration;
	private long size;
	private long bit_rate;
	private int probe_score;
	private Map<String, String> tags;

}
