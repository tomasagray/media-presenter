package self.me.mp.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import self.me.mp.db.VideoRepository;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoService {

	private final VideoRepository videoRepository;
	private final FFmpegPlugin ffmpegPlugin;
	private final RecursiveWatcherService watcherService;

	public VideoService(
			VideoRepository videoRepository,
			FFmpegPlugin ffmpegPlugin,
			RecursiveWatcherService watcherService) {
		this.videoRepository = videoRepository;
		this.ffmpegPlugin = ffmpegPlugin;
		this.watcherService = watcherService;
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

	public Video updateVideo(@NotNull Video video) {
		return videoRepository.save(video);
	}

	public void deleteVideo(@NotNull Video video) {
		videoRepository.delete(video);
	}

	public Video updateVideoMetadata(@NotNull Video video) throws IOException {
		final URI videoUri = video.getUri();
		final FFmpegMetadata metadata = ffmpegPlugin.readFileMetadata(videoUri);
		video.setMetadata(metadata);
		return video;
	}

	public List<Video> updateAllVideoMetadata() throws IOException {
		Page<Video> page;
		List<Video> updated = new ArrayList<>();
		do {
			page = fetchAll(0, 100);
			final List<Video> videos = page.getContent();
			for (Video video : videos) {
				updated.add(updateVideoMetadata(video));
			}
		} while (page.hasNext());
		return updated;
	}
}
