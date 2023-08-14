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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
@Transactional
public class VideoService {

	private static final Logger logger = LogManager.getLogger(VideoService.class);

	private final VideoRepository videoRepository;
	private final RecursiveWatcherService watcherService;
	private final FileScanningService<Video> videoScanningService;

	@Value("${videos.location}")
	private Path videoLocation;

	public VideoService(
			VideoRepository videoRepository,
			RecursiveWatcherService watcherService,
			FileScanningService<Video> videoScanningService) {
		this.videoRepository = videoRepository;
		this.watcherService = watcherService;
		this.videoScanningService = videoScanningService;
	}

	@Async("watcher")
	public void init(@Nullable Procedure onFinish) throws IOException {
		logger.info("Initializing videos in: {}", videoLocation);
		initializeVideoLocation();
		List<Video> existing = videoRepository.findAll();
		watcherService.watch(
				videoLocation,
				file -> videoScanningService.scanFile(file, existing, this::save),
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
				List<Video> existing = videoRepository.findAll();
				watcherService.walkTreeAndSetWatches(
						path,
						file -> videoScanningService.scanFile(file, existing, this::save),
						null,
						this::handleVideoFileEvent
				);
			} else {
				logger.info("Adding new video: {}", path);
				videoScanningService.scanFile(path, new ArrayList<>(), this::save);
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

	public Video save(@NotNull Video video) {
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

	public UrlResource getVideoThumb(@NotNull UUID videoId, @NotNull UUID thumbId) {
		return getById(videoId)
				.map(Video::getThumbnails)
				.map(thumbs -> thumbs.getImage(thumbId))
				.map(Image::getUri)
				.map(VideoService::toUrl)
				.map(UrlResource::new)
				.orElseThrow();
	}

	public MultiValueMap<String, Path> getInvalidFiles() {
		return videoScanningService.getInvalidFiles();
	}
}
