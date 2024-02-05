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
import self.me.mp.model.Picture;
import self.me.mp.model.Tag;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class PictureScanningService implements FileScanningService {

	private static final Logger logger = LogManager.getLogger(PictureScanningService.class);

	private static final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();


	private final PictureService pictureService;
	private final TagService tagService;
	private final FileUtilitiesService fileUtilitiesService;
	private final RecursiveWatcherService watcherService;
	private final FileTransferWatcher transferWatcher;

	@Value("${pictures.location}")
	private Path pictureLocation;

	private final List<Picture> scannedPictures = new ArrayList<>();

	public PictureScanningService(
			PictureService pictureService, TagService tagService,
			FileUtilitiesService fileUtilitiesService, RecursiveWatcherService watcherService,
			FileTransferWatcher transferWatcher) {
		this.pictureService = pictureService;
		this.tagService = tagService;
		this.fileUtilitiesService = fileUtilitiesService;
		this.watcherService = watcherService;
		this.transferWatcher = transferWatcher;
	}

	@Override
	public void scanFile(@NotNull Path file, @NotNull Collection<Path> existing) {
		try {
			fileUtilitiesService.repairFilename(file);
			if (!existing.contains(file)) {
				Picture picture = Picture.pictureBuilder()
						.uri(file.toUri())
						.title(FilenameUtils.getBaseName(file.toString()))
						.build();
				scannedPictures.add(picture);
				logger.info("Adding new Picture: {}", picture);
			} else {
				logger.trace("Picture already exists: {}", file);
			}
		} catch (Throwable e) {
			logger.error("Found invalid Picture file: {}", file);
			String ext = FilenameUtils.getExtension(file.toString());
			invalidFiles.add(ext, file);
		}
	}

	@Override
	public synchronized void saveScannedData() {
		List<Picture> savable = scannedPictures.stream().filter(Objects::nonNull).toList();
		logger.info("Saving {} newly scanned Pictures to database...", savable.size());
		pictureService.saveAll(savable);
		scannedPictures.clear();
	}

	@Async("transcoder")
	public void processImageMetadata(@NotNull Picture picture) {
		try {
			URI uri = picture.getUri();
			// get tags from sub dirs
			List<Tag> tags = tagService.getTags(pictureLocation.relativize(Path.of(uri)));
			picture.getTags().addAll(tags);

			BufferedImage image = ImageIO.read(uri.toURL());
			picture.setFilesize(new File(uri).length());
			picture.setWidth(image.getWidth());
			picture.setHeight(image.getHeight());
			pictureService.save(picture);
		} catch (Throwable e) {
			logger.error("Could not process Picture: {}", e.getMessage());
		}
	}

	@Override
	public void handleFileEvent(@NotNull Path file, @NotNull WatchEvent.Kind<?> kind) {
		logger.info("Event: {} happened to picture: {}", kind, file);
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(file)) {
				logger.info("Detected new Picture directory: {}", file);
				watcherService.walkTreeAndSetWatches(
						file,
						path -> this.scanFile(path, new ArrayList<>()),
						this::handleFileEvent,
						this::processScannedPictures
				);
			} else {
				logger.info("Found new Picture: {}", file);
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			transferWatcher.watchFileTransfer(file, doneFile -> {
				scanFile(doneFile, new ArrayList<>());
				processScannedPictures();
			});
		} else if (ENTRY_DELETE.equals(kind)) {
			logger.info("Deleting Picture at: {}", file);
			String ext = FilenameUtils.getExtension(file.toString());
			List<Path> invalidPaths = this.getInvalidFiles().get(ext);
			if (invalidPaths != null) {
				boolean removed = invalidPaths.remove(file);
				if (removed) {
					logger.info("Invalid file: {} deleted", file);
					return;
				}
			}
			pictureService.getPictureByPath(file).forEach(pic -> pictureService.deletePicture(pic.getId()));
		}
	}

	private void processScannedPictures() {
		List<Picture> pics = new ArrayList<>(scannedPictures);
		saveScannedData();
		for (Picture picture : pics) {
			processImageMetadata(picture);
		}
	}

	@Override
	public MultiValueMap<String, Path> getInvalidFiles() {
		return invalidFiles;
	}
}
