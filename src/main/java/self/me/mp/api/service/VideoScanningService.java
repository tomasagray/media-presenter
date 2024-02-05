package self.me.mp.api.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import self.me.mp.model.Tag;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class VideoScanningService implements ConvertFileScanningService<Video> {

	private static final Logger logger = LogManager.getLogger(VideoScanningService.class);

	private final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	private final VideoService videoService;
	private final ThumbnailService thumbnailService;
	private final TagService tagService;
	private final TranscodingService transcodingService;
	private final FileUtilitiesService fileUtilitiesService;
	private final RecursiveWatcherService watcherService;
	private final FileTransferWatcher transferWatcher;

	@Value("${videos.storage-location}")
	private Path videoStorageLocation;

	private final List<Video> scannedVideos = new ArrayList<>();

	VideoScanningService(
			VideoService videoService, ThumbnailService thumbnailService,
			TagService tagService, TranscodingService transcodingService,
			RecursiveWatcherService watcherService, FFmpegPlugin ffmpegPlugin,
			FileUtilitiesService fileUtilitiesService, FileTransferWatcher transferWatcher) {
		this.videoService = videoService;
		this.thumbnailService = thumbnailService;
		this.tagService = tagService;
		this.transcodingService = transcodingService;
		this.fileUtilitiesService = fileUtilitiesService;
		this.watcherService = watcherService;
		this.transferWatcher = transferWatcher;
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
			if (transcodingService.requiresTranscode(video)) {
				transcodingService.transcodeVideo(video,
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
			final FFmpegMetadata metadata;
			metadata = transcodingService.getVideoMetadata(video);
			video.setMetadata(metadata);

			// title
			final Path file = video.getFile();
			final String title = transcodingService.getTitle(metadata);
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
}
