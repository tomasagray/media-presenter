package self.me.mp.api.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import self.me.mp.db.VideoRepository;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class VideoService {

	private static final Logger logger = LogManager.getLogger(VideoService.class);

	private final VideoRepository videoRepository;
	private final FFmpegPlugin ffmpegPlugin;
	private final RecursiveWatcherService watcherService;

	@Value("${videos.location}")
	private Path videoLocation;

	public VideoService(
			VideoRepository videoRepository,
			FFmpegPlugin ffmpegPlugin,
			RecursiveWatcherService watcherService) {
		this.videoRepository = videoRepository;
		this.ffmpegPlugin = ffmpegPlugin;
		this.watcherService = watcherService;
	}

	public void init() throws IOException {
		logger.info("Initializing videos in: {}", videoLocation);

		Set<Path> existing =
				videoRepository.findAll()
						.stream()
						.map(Video::getFile)
						.collect(Collectors.toSet());
		watcherService.watch(
				videoLocation,
				file -> scanVideoFile(file, existing),
				this::handleVideoFileEvent
		);
	}

	private void scanVideoFile(@NotNull Path file, @NotNull Collection<Path> existing) {

		try {
			if (!existing.contains(file)) {
				String name = FilenameUtils.getBaseName(file.toString());
				logger.info("Adding new video: {}", name);
				Video video = new Video(name, file);
				updateVideoMetadata(video);
				// TODO: generate video thumbnail here
				saveVideo(video);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void handleVideoFileEvent(@NotNull Path path, @NotNull WatchEvent.Kind<?> kind) {
		if (kind.equals(ENTRY_CREATE)) {
			logger.info("Adding new video: {}", path);
		} else if (kind.equals(ENTRY_MODIFY)) {
			logger.info("Updating video: {}", path);
		} else if (kind.equals(ENTRY_DELETE)) {
			logger.info("Deleting Video: {}", path);
		}
	}

	public Video saveVideo(@NotNull Video video) {
		return videoRepository.save(video);
	}

	public Page<Video> fetchAll(int page, int pageSize) {
		return videoRepository.findAll(PageRequest.of(page, pageSize));
	}

	public Page<Video> fetchLatest(int page, int pageSize) {
		return videoRepository.findAllByOrderByAddedDesc(PageRequest.of(page, pageSize));
	}

	public Optional<Video> fetchById(@NotNull UUID videoId) {
		return videoRepository.findById(videoId);
	}

	public UrlResource getVideoData(@NotNull UUID videoId) throws MalformedURLException {
		logger.info("Reading video data for: {}", videoId);
		Optional<Video> videoOptional = fetchById(videoId);
		if (videoOptional.isPresent()) {
			Video video = videoOptional.get();
			return new UrlResource(video.getFile().toUri());
		}
		throw new IllegalArgumentException("Video not found: " + videoId);
	}

	public Video updateVideo(@NotNull Video video) {
		return videoRepository.save(video);
	}

	public void deleteVideo(@NotNull Video video) {
		videoRepository.delete(video);
	}

	public Video updateVideoMetadata(@NotNull Video video) throws IOException {
		final URI videoUri = video.getFile().toUri();
		final FFmpegMetadata metadata = ffmpegPlugin.readFileMetadata(videoUri);
		video.setMetadata(metadata);
		return video;
	}
}
