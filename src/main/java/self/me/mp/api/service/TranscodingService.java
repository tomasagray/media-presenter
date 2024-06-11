package self.me.mp.api.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;
import self.me.mp.plugin.ffmpeg.LoggableThread;
import self.me.mp.plugin.ffmpeg.SimpleTranscodeRequest;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegFormat;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegStream;

@Service
public class TranscodingService {

	private static final Logger logger = LogManager.getLogger(TranscodingService.class);

	private static final List<String> SUPPORTED_VCODECS = List.of("h264");
	private static final List<String> SUPPORTED_ACODECS = List.of("aac");
	private static final List<String> SUPPORTED_CONTAINERS = List.of("mp4");

	private final FFmpegPlugin ffmpegPlugin;

	@Value("${videos.convert-location}")
	private Path convertLocation;

	public TranscodingService(FFmpegPlugin fFmpegPlugin) {
		this.ffmpegPlugin = fFmpegPlugin;
	}

	@NotNull
	private static String getVideoTitleFromFilename(@NotNull Video video) {
		Path file = video.getFile();
		if (file != null) {
			String fileName = file.getFileName().toString();
			if (fileName.contains(".")) {
				return fileName.substring(0, fileName.lastIndexOf('.'));
			}
			return fileName;
		}
		return "";
	}

	private static boolean isCorrectType(@NotNull FFmpegStream stream, @NotNull String type) {
		return stream.getCodec_type() != null && type.equals(stream.getCodec_type());
	}

	public boolean requiresTranscode(@NotNull Video video) throws IOException {
		if (video.getMetadata() == null) {
			final FFmpegMetadata metadata = getVideoMetadata(video);
			video.setMetadata(metadata);
		}
		return !checkCorrectVideoFormat(video.getMetadata());
	}

	@Async("transcoder")
	public void transcodeVideo(
			@NotNull Video video,
			@NotNull Consumer<? super Path> onSucceed,
			@NotNull Consumer<? super Path> onFail) {
		// prepare arguments
		final FFmpegMetadata metadata = video.getMetadata();
		final String videoCodec = getTranscodeVideoCodec(metadata);
		final String audioCodec = getTranscodeAudioCodec(metadata);
		final Path filename = getFilenameFromTitle(video);
		final Path convertPath = convertLocation.resolve(filename);
		final String title = getVideoTitleFromFilename(video);

		final SimpleTranscodeRequest request = SimpleTranscodeRequest.builder()
				.from(video.getFile().toUri())
				.to(convertPath)
				.videoCodec(videoCodec)
				.audioCodec(audioCodec)
				.additionalArgs(Map.of("-metadata", "title=" + title))
				.build();

		// perform transcode
		final LoggableThread streamTask = ffmpegPlugin.transcode(request);
		streamTask.onLoggableEvent(logger::trace)
				.onError(logger::error)
				.onComplete(exitCode -> {
					if (exitCode == 0) {
						onSucceed.accept(convertPath);
					} else {
						onFail.accept(convertPath);
					}
				})
				.start();
	}

	@NotNull
	private Path getFilenameFromTitle(@NotNull Video video) {
		String filename;
		String title = getTitle(video.getMetadata());
		if (title != null) {
			filename = String.format("%s.%s", title, SUPPORTED_CONTAINERS.get(0));
		} else {
			String baseName = FilenameUtils.getBaseName(video.getFile().toString());
			filename = String.format("%s.%s", baseName, SUPPORTED_CONTAINERS.get(0));
		}
		return Path.of(filename);
	}

	private String getTranscodeVideoCodec(FFmpegMetadata metadata) {
		String codec = getCodec(metadata, "video");
		return codec != null && SUPPORTED_VCODECS.contains(codec) ?
				"copy" :
				SUPPORTED_VCODECS.get(0);
	}

	private String getTranscodeAudioCodec(FFmpegMetadata metadata) {
		String codec = getCodec(metadata, "audio");
		return codec != null && SUPPORTED_ACODECS.contains(codec) ?
				"copy" :
				SUPPORTED_ACODECS.get(0);
	}

	private boolean checkCorrectVideoFormat(FFmpegMetadata metadata) {
		final String title = getTitle(metadata);
		final String container = getContainer(metadata);
		final String videoCodec = getCodec(metadata, "video");
		final String audioCodec = getCodec(metadata, "audio");
		return title != null && !title.isEmpty() &&
				container != null && SUPPORTED_CONTAINERS.contains(container) &&
				videoCodec != null && SUPPORTED_VCODECS.contains(videoCodec) &&
				audioCodec != null && SUPPORTED_ACODECS.contains(audioCodec);
	}

	public @Nullable String getTitle(FFmpegMetadata metadata) {
		if (metadata == null) return null;
		final FFmpegFormat format = metadata.getFormat();
		if (format == null) return null;
		return findTitle(format.getTags());
	}

	private @Nullable String findTitle(@Nullable Map<String, String> tags) {
		if (tags == null) return null;
		final List<String> variations = List.of("TITLE", "title");
		for (String variant : variations) {
			String tag = tags.get(variant);
			if (tag != null) return tag;
		}
		return null;
	}

	private @Nullable String getContainer(FFmpegMetadata metadata) {
		if (metadata == null) return null;
		final FFmpegFormat format = metadata.getFormat();
		if (format == null) return null;
		return format.getFormat_name();
	}

	private @Nullable String getCodec(FFmpegMetadata metadata, @NotNull String type) {
		if (metadata == null) return null;
		final List<FFmpegStream> streams = metadata.getStreams();
		if (streams == null || streams.isEmpty()) return null;
		return streams.stream()
				.filter(stream -> isCorrectType(stream, type))
				.map(FFmpegStream::getCodec_name)
				.findAny()
				.orElse(null);
	}

	public FFmpegMetadata getVideoMetadata(@NotNull Video video) throws IOException {
		final URI videoUri = video.getFile().toUri();
		return ffmpegPlugin.readFileMetadata(videoUri);
	}
}
