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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import self.me.mp.db.PictureRepository;
import self.me.mp.model.Picture;

import java.io.File;
import java.io.IOException;
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
public class PictureService {

	private static final Logger logger = LogManager.getLogger(PictureService.class);
	private final PictureRepository pictureRepo;
	private final FileScanningService<Picture> scanningService;
	private final RecursiveWatcherService watcherService;

	@Value("${pictures.location}")
	private Path pictureLocation;

	public PictureService(
			PictureRepository pictureRepo,
			RecursiveWatcherService watcherService,
			PictureScanningService scanningService) {
		this.pictureRepo = pictureRepo;
		this.watcherService = watcherService;
		this.scanningService = scanningService;
	}

	@Async("watcher")
	public void init() throws IOException {
		initializePictureLocation();
		logger.info("Scanning Picture files in: {}", pictureLocation);
		List<Picture> existing = pictureRepo.findAll();
		watcherService.watch(
				pictureLocation,
				picture -> scanningService.scanFile(picture, existing, this::save),
				this::handleFileEvent
		);
	}

	private void initializePictureLocation() throws IOException {
		File file = pictureLocation.toFile();
		if (!file.exists()) {
			logger.info("Picture storage location: {} does not exist; creating...", pictureLocation);
			if (!file.mkdirs()) {
				throw new IOException("Could not create location for Picture storage: " + pictureLocation);
			}
		}
	}

	public MultiValueMap<String, Path> getInvalidFiles() {
		return scanningService.getInvalidFiles();
	}

	public Picture save(@NotNull Picture picture) {
		return pictureRepo.save(picture);
	}

	private void handleFileEvent(@NotNull Path file, @NotNull WatchEvent.Kind<?> kind) {
		logger.info("Event: {} happened to picture: {}", kind, file);
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(file)) {
				logger.info("Detected new Picture directory: {}", file);
				List<Picture> existing = pictureRepo.findAll();
				watcherService.walkTreeAndSetWatches(
						file,
						path -> scanningService.scanFile(path, existing, this::save),
						null,
						this::handleFileEvent
				);
			} else {
				logger.info("Found new Picture: {}", file);
				scanningService.scanFile(file, new ArrayList<>(), this::save);
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			// TODO: handle modify picture
			logger.info("Picture was modified: {}", file);
		} else if (ENTRY_DELETE.equals(kind)) {
			logger.info("Deleting Picture at: {}", file);
			String ext = FilenameUtils.getExtension(file.toString());
			List<Path> invalidPaths = scanningService.getInvalidFiles().get(ext);
			if (invalidPaths != null) {
				boolean removed = invalidPaths.remove(file);
				if (removed) {
					logger.info("Invalid file: {} deleted", file);
					return;
				}
			}
			getPictureByPath(file).forEach(pic -> deletePicture(pic.getId()));
		}
	}

	public Page<Picture> getLatestPictures(int page, int pageSize) {
		return pictureRepo.findLatest(PageRequest.of(page, pageSize));
	}

	public List<Picture> getRandomPictures(int count) {
		return pictureRepo.findRandom(PageRequest.ofSize(count));
	}

	public Optional<Picture> getPicture(@NotNull UUID picId) {
		return pictureRepo.findById(picId);
	}

	public Optional<UrlResource> getPictureData(@NotNull UUID picId) {
		return getPicture(picId).map(img -> UrlResource.from(img.getUri()));
	}

	public List<Picture> getPictureByPath(@NotNull Path path) {
		return pictureRepo.findAll().stream().filter(pic ->
				pic.getUri().getPath().startsWith(path.toString())
		).toList();
	}

	public void deletePicture(@NotNull UUID picId) {
		logger.info("Deleting Picture: {}", picId);
		pictureRepo.deleteById(picId);
	}

}
