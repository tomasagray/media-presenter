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
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class VideoScanningService implements ConvertFileScanningService<Video> {

	private static final Logger logger = LogManager.getLogger(VideoScanningService.class);

	private static final List<String> SUPPORTED_VCODECS = List.of("h264");
	private static final List<String> SUPPORTED_ACODECS = List.of("aac");
	private static final List<String> SUPPORTED_CONTAINERS = List.of("mp4");

	private final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	private final VideoService videoService;
	private final ThumbnailService thumbnailService;
	private final TagService tagService;
	private final FileUtilitiesService fileUtilitiesService;
	private final RecursiveWatcherService watcherService;
	private final FileTransferWatcher transferWatcher;
	private final FFmpegPlugin ffmpegPlugin;        // TODO: refactor transcoding to its own service

	@Value("${videos.storage-location}")
	private Path videoStorageLocation;

	@Value("${videos.convert-location}")
	private Path convertLocation;

	private final List<Video> scannedVideos = new ArrayList<>();

	VideoScanningService(
			VideoService videoService, ThumbnailService thumbnailService,
			TagService tagService, RecursiveWatcherService watcherService, FFmpegPlugin ffmpegPlugin,
			FileUtilitiesService fileUtilitiesService, FileTransferWatcher transferWatcher) {
		this.videoService = videoService;
		this.thumbnailService = thumbnailService;
		this.tagService = tagService;
		this.fileUtilitiesService = fileUtilitiesService;
		this.watcherService = watcherService;
		this.ffmpegPlugin = ffmpegPlugin;
		this.transferWatcher = transferWatcher;
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

	@Override
	public void scanFile(@NotNull Path file, @NotNull Collection<Path> existing) {
		try {
			fileUtilitiesService.repairFilename(file);
			logger.trace("Attempting to scan video file: {}", file);
			if (!existing.contains(file)) {
				logger.info("Adding new video: {}", file);
				final Video video = new Video(file);
				scannedVideos.add(video);
			} else {
				logger.trace("Video file: {} has already been scanned", file);
			}
		} catch (Throwable e) {
			logger.error("Error scanning video: {}", e.getMessage(), e);
			String ext = FilenameUtils.getExtension(file.toString());
			invalidFiles.add(ext, file);
		}
	}

	@Override
	public void scanAddFile(@NotNull Path file) {
		logger.info("Attempting to add new video file: {}", file);
		try {
			fileUtilitiesService.repairFilename(file);
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

	@Override
	public synchronized void saveScannedData() {
		List<Video> savable = scannedVideos.stream().filter(Objects::nonNull).toList();
		logger.info("Saving {} Videos to database...", savable.size());
		videoService.saveAll(savable);
		scannedVideos.clear();
	}

	@Async("transcoder")
	public void scanVideoMetadata(@NotNull Video video) {
		try {
			// metadata
			final FFmpegMetadata metadata = getVideoMetadata(video);
			video.setMetadata(metadata);

			// title
			final Path file = video.getFile();
			final String title = getTitle(metadata);
			if (title != null) video.setTitle(title);
			else video.setTitle(FilenameUtils.getBaseName(file.toString()));

			// set tags
			final List<Tag> tags = tagService.getTags(videoStorageLocation.relativize(file));
			video.setTags(new HashSet<>(tags));

			// thumbnails
			thumbnailService.generateVideoThumbnails(video);
			videoService.save(video);
		} catch (IOException e) {
			logger.error("Could not scan Video metadata: {}", e.getMessage(), e);
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
		final Path filename = file.getFileName();
		final File videoFile = videoStorageLocation.resolve(filename).toFile();
		logger.info("Renaming video file {} to: {}", file, videoFile);
		final boolean renamed = file.toFile().renameTo(videoFile);
		if (!renamed) {
			throw new IOException("Could not move video from 'add' to 'storage'");
		}
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

	public void handleAddVideoEvent(@NotNull Path path, @NotNull WatchEvent.Kind<?> kind) {
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(path)) {
				watcherService.walkTreeAndSetWatches(
						path,
						this::scanAddFile,
						this::handleAddVideoEvent,
						null
				);
			} else {
				logger.info("Found video to add: {}", path);
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			transferWatcher.watchFileTransfer(path, this::scanAddFile);
		} else if (ENTRY_DELETE.equals(kind)) {
			logger.info("File deleted from video add directory: {}", path);
		}
	}

	@Override
	public void handleFileEvent(@NotNull Path path, @NotNull WatchEvent.Kind<?> kind) {
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(path)) {
				watcherService.walkTreeAndSetWatches(
						path,
						dir -> this.scanFile(dir, new ArrayList<>()),
						this::handleFileEvent,
						this::processScannedVideos
				);
			} else {
				logger.info("Adding new video: {}", path);
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			transferWatcher.watchFileTransfer(path, doneFile -> {
				scanFile(doneFile, new ArrayList<>());
				processScannedVideos();
			});
		} else if (ENTRY_DELETE.equals(kind)) {
			videoService.getVideoByPath(path)
					.ifPresentOrElse(
							videoService::deleteVideo,
							() -> logger.info("Deleted video that was not in DB: {}", path)
					);
		}
	}

	private void processScannedVideos() {
		List<Video> videos = new ArrayList<>(scannedVideos);
		saveScannedData();
		for (Video video : videos) {
			scanVideoMetadata(video);
		}
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

	private @Nullable String getTitle(FFmpegMetadata metadata) {
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

	private FFmpegMetadata getVideoMetadata(@NotNull Video video) throws IOException {
		final URI videoUri = video.getFile().toUri();
		return ffmpegPlugin.readFileMetadata(videoUri);
	}
}
