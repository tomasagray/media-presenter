package self.me.mp.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import self.me.mp.Procedure;
import self.me.mp.db.VideoRepository;
import self.me.mp.model.Image;
import self.me.mp.model.UserPreferences;
import self.me.mp.model.UserVideoView;
import self.me.mp.model.Video;

import java.io.File;
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
@Transactional
public class VideoService {

	private static final Logger logger = LogManager.getLogger(VideoService.class);

	private final VideoRepository videoRepository;
	private final RecursiveWatcherService watcherService;
	private final VideoScanningService videoScanningService;
	private final UserService userService;

	@Value("${videos.location}")
	private Path videoLocation;

	public VideoService(
			VideoRepository videoRepository,
			RecursiveWatcherService watcherService,
			VideoScanningService videoScanningService, UserService userService) {
		this.videoRepository = videoRepository;
		this.watcherService = watcherService;
		this.videoScanningService = videoScanningService;
		this.userService = userService;
	}

	@Async("watcher")
	public void init(@Nullable Procedure onFinish) throws IOException {
		logger.info("Initializing videos in: {}", videoLocation);
		initializeVideoLocation();
		Set<Path> existing = getExistingVideos();
		watcherService.watch(
				videoLocation,
				file -> videoScanningService.scanVideoFile(file, existing, this::saveVideo),
				onFinish,
				this::handleVideoFileEvent
		);
	}

	private void initializeVideoLocation() throws IOException {
		File file = videoLocation.toFile();
		if (!file.exists()) {
			logger.info("Video storage location: {} does not exist; creating...", videoLocation);
			if (!file.mkdirs()) {
				throw new IOException("Could not create location for Video storage: " + videoLocation);
			}
		}
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

	private void handleVideoFileEvent(@NotNull Path path, @NotNull WatchEvent.Kind<?> kind) {
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(path)) {
				watcherService.walkTreeAndSetWatches(
						path,
						file -> videoScanningService.scanVideoFile(file, getExistingVideos(), this::saveVideo),
						null,
						this::handleVideoFileEvent
				);
			} else {
				logger.info("Adding new video: {}", path);
				videoScanningService.scanVideoFile(path, new ArrayList<>(), this::saveVideo);
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

	public void saveVideo(@NotNull Video video) {
		videoRepository.save(video);
	}

	public Page<Video> getAll(int page, int pageSize) {
		return videoRepository.findAll(PageRequest.of(page, pageSize));
	}

	public Page<UserVideoView> getAllUserVideos(int page, int size) {
		return getAll(page, size).map(this::getUserVideoView);
	}

	public Page<Video> getLatest(int page, int pageSize) {
		return videoRepository.findAllByOrderByAddedDesc(PageRequest.of(page, pageSize));
	}

	public Page<UserVideoView> getLatestUserVideos(int page, int size) {
		return getLatest(page, size).map(this::getUserVideoView);
	}

	public UserVideoView getUserVideoView(@NotNull Video video) {
		return userService.getUserPreferences().isFavorite(video) ?
				UserVideoView.favorite(video) : UserVideoView.of(video);
	}

	public Collection<UserVideoView> getUserVideoViews(@NotNull Collection<Video> videos) {
		return videos.stream().map(this::getUserVideoView).toList();
	}

	public List<Video> getRandom(int count) {
		return videoRepository.findRandom(PageRequest.ofSize(count));
	}

	public List<UserVideoView> getRandomUserVideos(int count) {
		return getRandom(count).stream().map(this::getUserVideoView).toList();
	}

	public Optional<Video> getById(@NotNull UUID videoId) {
		return videoRepository.findById(videoId);
	}

	public Optional<UserVideoView> getUserVideo(@NotNull UUID videoId) {
		return getById(videoId).map(this::getUserVideoView);
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


	public UrlResource getVideoThumb(@NotNull UUID videoId, @NotNull UUID thumbId) {
		return getById(videoId)
				.map(Video::getThumbnails)
				.map(thumbs -> thumbs.getImage(thumbId))
				.map(Image::getUri)
				.map(VideoService::toUrl)
				.map(UrlResource::new)
				.orElseThrow();
	}

	public UserVideoView toggleVideoFavorite(@NotNull UUID videoId) {
		Optional<Video> optional = getById(videoId);
		if (optional.isEmpty()) {
			throw new IllegalArgumentException("Trying to favorite non-existent Video: " + videoId);
		}
		Video video = optional.get();
		UserPreferences preferences = userService.getUserPreferences();
		if (preferences.toggleFavorite(video)) {
			return UserVideoView.favorite(video);
		}
		return UserVideoView.of(video);
	}

	public Collection<UserVideoView> getVideoFavorites() {
		return userService.getUserPreferences()
				.getFavoriteVideos().stream()
				.map(this::getUserVideoView)
				.toList();
	}

	public MultiValueMap<String, Path> getInvalidFiles() {
		return videoScanningService.getInvalidFiles();
	}
}
