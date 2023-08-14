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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

@Service
public class VideoScanningService implements FileScanningService<Video> {

	private static final Logger logger = LogManager.getLogger(VideoScanningService.class);

	private final FFmpegPlugin ffmpegPlugin;
	private final ThumbnailService thumbnailService;
	private final TagService tagService;

	private final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	@Value("${videos.location}")
	private Path videoLocation;

	public VideoScanningService(
			FFmpegPlugin ffmpegPlugin,
			ThumbnailService thumbnailService,
			TagService tagService) {
		this.ffmpegPlugin = ffmpegPlugin;
		this.thumbnailService = thumbnailService;
		this.tagService = tagService;
	}

	private static void renameToSafeFilename(@NotNull Path file)
			throws IOException {
		String name = file.toString();
		String newName = name.replace(" ", "_");
		logger.info("Renaming file: {} to: {}", file, newName);
		Files.move(file, file.resolveSibling(newName));
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
				String name = FilenameUtils.getBaseName(file.toString());
				if (name.contains(" ")) {
					renameToSafeFilename(file);
					return;     // change will be picked up by watcher
				}
				logger.info("Adding new video: {}", file);
				String correctedName = name.replace("_", " ");
				Video video = new Video(correctedName, file);
				updateVideoMetadata(video);
				List<Tag> tags = tagService.getTags(videoLocation.relativize(file));
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

	private void updateVideoMetadata(@NotNull Video video) throws IOException {
		final URI videoUri = video.getFile().toUri();
		final FFmpegMetadata metadata = ffmpegPlugin.readFileMetadata(videoUri);
		video.setMetadata(metadata);
	}
}
