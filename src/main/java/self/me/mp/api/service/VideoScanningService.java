package self.me.mp.api.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import self.me.mp.model.Tag;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;
import self.me.mp.plugin.ffmpeg.LoggableThread;
import self.me.mp.plugin.ffmpeg.SimpleTranscodeRequest;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegFormat;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegStream;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

@Service
public class VideoScanningService implements ConvertFileScanningService<Video> {

	private static final Logger logger = LogManager.getLogger(VideoScanningService.class);

	private static final List<String> SUPPORTED_VCODECS = List.of("h264");
	private static final List<String> SUPPORTED_ACODECS = List.of("aac");
	private static final List<String> SUPPORTED_CONTAINERS = List.of("mp4");

	private final FFmpegPlugin ffmpegPlugin;
	private final ThumbnailService thumbnailService;
	private final TagService tagService;
	private final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	@Value("${videos.storage-location}")
	private Path videoStorageLocation;

	@Value("${videos.convert-location}")
	private Path convertLocation;

	public VideoScanningService(
			FFmpegPlugin ffmpegPlugin,
			ThumbnailService thumbnailService,
			TagService tagService) {
		this.ffmpegPlugin = ffmpegPlugin;
		this.thumbnailService = thumbnailService;
		this.tagService = tagService;
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

	private static void handleTranscodeVideoFailed(Video video, @NotNull Path converted) {
		logger.error("Transcoding video failed: {}", video);
		try {
			if (converted.toFile().exists()) {
				Files.delete(converted);
				// todo - delete transcode log,
				// delete converted data, throw error
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public MultiValueMap<String, Path> getInvalidFiles() {
		return new LinkedMultiValueMap<>(invalidFiles).deepCopy();
	}

	@Async("fileScanner")
	@Override
	public void scanFile(
			@NotNull Path file,
			@NotNull Collection<Video> existing,
			@NotNull Consumer<Video> onSave) {
		try {
			List<Path> existingPaths = existing.stream().map(Video::getFile).toList();
			if (!existingPaths.contains(file)) {
				logger.info("Adding new video: {}", file);

				final Video video = new Video(file);
				FFmpegMetadata metadata = getVideoMetadata(video);
				video.setMetadata(metadata);
				final String title = getTitle(metadata);
				if (title != null) video.setTitle(title);
				else video.setTitle(FilenameUtils.getBaseName(file.toString()));

				final List<Tag> tags = tagService.getTags(videoStorageLocation.relativize(file));
				video.setTags(new HashSet<>(tags));

				onSave.accept(video);   // ensure ID set
				thumbnailService.generateVideoThumbnails(video);
				onSave.accept(video);   // save thumbs
			}
		} catch (Throwable e) {
			logger.error("Error scanning video: {}", e.getMessage(), e);
			String ext = FilenameUtils.getExtension(file.toString());
			invalidFiles.add(ext, file);
		}
	}

	@Async("fileScanner")
	@Override
	public void scanAddFile(@NotNull Path file) {
		try {
			final Video video = new Video(file);
			final FFmpegMetadata metadata = getVideoMetadata(video);
			video.setMetadata(metadata);
			final boolean correctVideoFormat = checkCorrectVideoFormat(metadata);
			if (!correctVideoFormat) {
				transcodeVideo(video,
						(converted) -> finalizeTranscodeVideo(file, converted),
						(converted) -> handleTranscodeVideoFailed(video, converted));
			} else {
				moveVideoToStorage(file);
			}
		} catch (Throwable e) {
			logger.error("Error scanning video file to add: {}", e.getMessage(), e);
		}
	}

	private void finalizeTranscodeVideo(@NotNull Path file, @NotNull Path converted) {
		try {
			if (converted.toFile().exists()) {
				moveVideoToStorage(converted);
				Files.delete(file); // delete original
			} else {
				throw new IOException("Could not locate transcoded video: " + converted);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void moveVideoToStorage(@NotNull Path file) throws IOException {
		final Path filename = getRandomVideoFilename("mp4");
		final File videoFile = videoStorageLocation.resolve(filename).toFile();
		logger.info("Renaming video file to: {}", videoFile);
		final boolean renamed = file.toFile().renameTo(videoFile);
		if (!renamed) {
			throw new IOException("Could not move video from 'add' to 'storage'");
		}
	}

	@Async("transcoder")
	public void transcodeVideo(
			@NotNull Video video,
			@NotNull Consumer<? super Path> onSucceed,
			@NotNull Consumer<? super Path> onFail) throws InterruptedException {
		// prepare arguments
		final FFmpegMetadata metadata = video.getMetadata();
		final String container = getTranscodeContainer(metadata);
		final String videoCodec = getTranscodeVideoCodec(metadata);
		final String audioCodec = getTranscodeAudioCodec(metadata);
		final Path filename = getRandomVideoFilename(container);
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
		// todo - handle logging
		streamTask.onLoggableEvent(e -> System.out.println("transcode: " + e));
		streamTask.start();
		int exitCode = streamTask.getProcess().waitFor();
		logger.info("Transcoding completed with exit code: {}", exitCode);
		if (exitCode == 0) {
			onSucceed.accept(convertPath);
		} else {
			onFail.accept(convertPath);
		}
	}

	@NotNull
	private Path getRandomVideoFilename(@Nullable String container) {
		if (container == null || "".equals(container)) {
			container = SUPPORTED_CONTAINERS.get(0);
		}
		final String filename = String.format("%s.%s", UUID.randomUUID(), container.toLowerCase());
		return Path.of(filename);
	}

	private String getTranscodeContainer(FFmpegMetadata metadata) {
		String container = getContainer(metadata);
		return container != null && SUPPORTED_CONTAINERS.contains(container) ?
				"mp4" :
				SUPPORTED_CONTAINERS.get(0);
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
		return title != null && !"".equals(title) &&
				container != null && SUPPORTED_CONTAINERS.contains(container) &&
				videoCodec != null && SUPPORTED_VCODECS.contains(videoCodec) &&
				audioCodec != null && SUPPORTED_ACODECS.contains(audioCodec);
	}

	private @Nullable String getTitle(FFmpegMetadata metadata) {
		if (metadata == null) return null;
		final FFmpegFormat format = metadata.getFormat();
		if (format == null) return null;
		return findTitle(format.getTags());
	}

	private @Nullable String findTitle(@NotNull Map<String, String> tags) {
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
		if (streams == null || streams.size() == 0) return null;
		return streams.stream()
				.filter(stream -> isCorrectType(stream, type))
				.map(FFmpegStream::getCodec_name)
				.findAny()
				.orElse(null);
	}

	private FFmpegMetadata getVideoMetadata(@NotNull Video video) throws IOException {
		final URI videoUri = video.getFile().toUri();
		return ffmpegPlugin.readFileMetadata(videoUri);
	}
}
