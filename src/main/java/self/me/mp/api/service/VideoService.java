package self.me.mp.api.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import self.me.mp.db.VideoRepository;
import self.me.mp.model.Image;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class VideoService {

	private static final Logger logger = LogManager.getLogger(VideoService.class);

	private final VideoRepository videoRepository;
	private final FFmpegPlugin ffmpegPlugin;
	private final RecursiveWatcherService watcherService;
	private final ThumbnailService thumbnailService;

	@Value("${videos.location}")
	private Path videoLocation;

	public VideoService(
			VideoRepository videoRepository,
			FFmpegPlugin ffmpegPlugin,
			RecursiveWatcherService watcherService,
			ThumbnailService thumbnailService) {
		this.videoRepository = videoRepository;
		this.ffmpegPlugin = ffmpegPlugin;
		this.watcherService = watcherService;
		this.thumbnailService = thumbnailService;
	}

	@Async
	public void init() throws IOException {
		logger.info("Initializing videos in: {}", videoLocation);
		Set<Path> existing = getExistingVideos();
		watcherService.watch(
				videoLocation,
				file -> scanVideoFile(file, existing),
				this::handleVideoFileEvent
		);
	}

	@NotNull
	private Set<Path> getExistingVideos() {
		return videoRepository.findAll()
				.stream()
				.map(Video::getFile)
				.collect(Collectors.toSet());
	}

	@NotNull
	private static URL toUrl(@NotNull URI uri) {
		try {
			return uri.toURL();
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void renameToSafeFilename(@NotNull Path file)
			throws IOException {
		String name = file.toString();
		String newName = name.replace(" ", "_");
		logger.info("Renaming file: {} to: {}", file, newName);
		Files.move(file, file.resolveSibling(newName));
	}

	private void scanVideoFile(@NotNull Path file, @NotNull Collection<Path> existing) {

		try {
			if (!existing.contains(file)) {
				String name = FilenameUtils.getBaseName(file.toString());
				if (name.contains(" ")) {
					renameToSafeFilename(file);
					return;     // change will be picked up by watcher
				}
				logger.info("Adding new video: {}", file);
				String correctedName = name.replace("_", " ");
				Video video = new Video(correctedName, file);
				updateVideoMetadata(video);
				saveVideo(video);   // ensure ID set
				thumbnailService.generateVideoThumbnails(video);
				saveVideo(video);   // save thumbs
			}
		} catch (Throwable e) {
			logger.info("Error scanning video: {}", e.getMessage(), e);
		}
	}

	private void handleVideoFileEvent(@NotNull Path path, @NotNull WatchEvent.Kind<?> kind) {
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(path)) {
				watcherService.walkTreeAndSetWatches(
						path,
						file -> scanVideoFile(file, getExistingVideos()),
						this::handleVideoFileEvent
				);
			} else {
				logger.info("Adding new video: {}", path);
				scanVideoFile(path, new ArrayList<>());
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			logger.info("Video was modified: {}", path);
			// TODO: handle modify video
		} else if (ENTRY_DELETE.equals(kind)) {
			getVideoByPath(path)
					.ifPresentOrElse(
							this::deleteVideo,
							() -> logger.info("Deleted video that was not in DB: {}", path)
					);
		}
	}

	public Video saveVideo(@NotNull Video video) {
		return videoRepository.save(video);
	}

	public Page<Video> getAll(int page, int pageSize) {
		return videoRepository.findAll(PageRequest.of(page, pageSize));
	}

	public Page<Video> getLatest(int page, int pageSize) {
		return videoRepository.findAllByOrderByAddedDesc(PageRequest.of(page, pageSize));
	}

	public List<Video> getRandom(int count) {
		return videoRepository.findRandom(PageRequest.ofSize(count));
	}

	public Optional<Video> getById(@NotNull UUID videoId) {
		return videoRepository.findById(videoId);
	}

	public UrlResource getVideoData(@NotNull UUID videoId) throws MalformedURLException {
		logger.info("Reading video data for: {}", videoId);
		Optional<Video> videoOptional = getById(videoId);
		if (videoOptional.isPresent()) {
			Video video = videoOptional.get();
			return new UrlResource(video.getFile().toUri());
		}
		throw new IllegalArgumentException("Video not found: " + videoId);
	}

	public Optional<Video> getVideoByPath(@NotNull Path path) {
		return videoRepository.findAll()
				.stream()
				.filter(video -> video.getFile().equals(path))
				.findFirst();
	}

	public void deleteVideo(@NotNull Video video) {
		logger.info("Deleting Video: {}", video);
		videoRepository.delete(video);
	}

	public Video updateVideoMetadata(@NotNull Video video) throws IOException {
		final URI videoUri = video.getFile().toUri();
		final FFmpegMetadata metadata = ffmpegPlugin.readFileMetadata(videoUri);
		video.setMetadata(metadata);
		return video;
	}

	public UrlResource getVideoThumb(@NotNull UUID videoId, @NotNull UUID thumbId) {
		return getById(videoId)
				.map(Video::getThumbnails)
				.map(thumbs -> thumbs.getImage(thumbId))
				.map(Image::getUri)
				.map(VideoService::toUrl)
				.map(UrlResource::new)
				.orElseThrow();
	}
}
