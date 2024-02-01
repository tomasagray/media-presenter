package self.me.mp.api.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import self.me.mp.model.Image;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegStream;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

@Service
public class ThumbnailService {

	private static final Logger logger = LogManager.getLogger(ThumbnailService.class);

	private final FFmpegPlugin ffmpeg;

	@Value("${video.thumbnails.location}")
	private Path thumbLocation;

	@Value("${video.thumbnails.width}")
	private int defaultWidth;

	@Value("${video.thumbnails.height}")
	private int defaultHeight;

	public ThumbnailService(FFmpegPlugin ffmpeg) {
		this.ffmpeg = ffmpeg;
	}

	public void generateVideoThumbnails(@NotNull Video video) throws IOException {

		logger.info("Creating thumbnail for Video: {}", video);

		Path thumb = createThumbDir(video);
		List<FFmpegStream> streams = video.getMetadata().getStreams();
		if (streams == null || streams.isEmpty()) {
			throw new IOException("Video has no streams: " + video);
		}

		double duration = streams.get(0).getDuration();
		int sliceFactor = getSliceFactor(duration);
		double offset = duration * 0.05;
		double sliceDuration = duration / sliceFactor;

		for (int i = 0; i < sliceFactor; i++) {
			double thumbPos = i * sliceDuration + offset;
			Image thumbnail = generateThumbnail(video, thumb.resolve(i + ".jpg"), (long) thumbPos);
			logger.info("Created thumbnail: {}", thumbnail);
			video.addThumbnail(thumbnail);
		}
	}

	public Image generateThumbnail(
			@NotNull Video video,
			@NotNull Path thumb,
			long thumbPos) throws IOException {
		Path thumbnail = ffmpeg.createThumbnail(
				video.getFile(),
				thumb,
				LocalTime.ofSecondOfDay(thumbPos),
				defaultWidth,
				defaultHeight
		);
		return Image.builder().uri(thumbnail.toUri())
				.width(defaultWidth)
				.height(defaultHeight)
				.filesize(thumbnail.toFile().length())
				.title(FilenameUtils.getBaseName(thumbnail.toString()))
				.build();
	}

	@NotNull
	private Path createThumbDir(@NotNull Video video) throws IOException {
		Path thumb = thumbLocation.resolve(video.getId().toString());
		if (!thumb.toFile().mkdirs()) {
			throw new IOException("Could not create thumbnail directory: " + thumb);
		}
		return thumb;
	}

	private int getSliceFactor(double duration) {
		if (duration <= 60.0d) {
			return 3;
		} else if (duration <= (3 * 60.0d)) {
			return 5;
		} else {
			return 10;
		}
	}
}
