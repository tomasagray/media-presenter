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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import self.me.mp.db.PictureRepository;
import self.me.mp.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
@Transactional
public class PictureService {

	private static final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	private static final Logger logger = LogManager.getLogger(PictureService.class);
	private final PictureRepository pictureRepo;
	private final RecursiveWatcherService watcherService;
	private final TagService tagService;
	private final UserService userService;

	@Value("${pictures.location}")
	private Path pictureLocation;

	public PictureService(
			PictureRepository pictureRepo,
			RecursiveWatcherService watcherService,
			TagService tagService, UserService userService) {
		this.pictureRepo = pictureRepo;
		this.watcherService = watcherService;
		this.tagService = tagService;
		this.userService = userService;
	}

	@Async
	public void init() throws IOException {
		initializePictureLocation();
		logger.info("Scanning Picture files in: {}", pictureLocation);
		Set<Path> existing = getExistingPictures();
		watcherService.watch(
				pictureLocation,
				picture -> scanPicture(picture, existing),
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

	@NotNull
	private Set<Path> getExistingPictures() {
		return pictureRepo.findAll()
				.stream()
				.map(Image::getUri)
				.map(Path::of)
				.collect(Collectors.toSet());
	}

	private void scanPicture(@NotNull Path file, @NotNull Collection<Path> existing) {
		try {
			if (!existing.contains(file)) {
				// get tags from sub dirs
				List<Tag> tags = tagService.getTags(pictureLocation.relativize(file));
				BufferedImage image = ImageIO.read(file.toFile());
				if (image != null) {
					Picture picture = Picture.pictureBuilder()
							.width(image.getWidth())
							.height(image.getHeight())
							.filesize(file.toFile().length())
							.uri(file.toUri())
							.title(FilenameUtils.getBaseName(file.toString()))
							.build();
					picture.getTags().addAll(tags);
					pictureRepo.save(picture);
				} else {
					throw new IOException("Image data was null");
				}
			}
		} catch (Throwable e) {
			String ext = FilenameUtils.getExtension(file.toString());
			invalidFiles.add(ext, file);
		}
	}

	public MultiValueMap<String, Path> getInvalidFiles() {
		return invalidFiles;
	}

	private void handleFileEvent(@NotNull Path file, @NotNull WatchEvent.Kind<?> kind) {
		logger.info("Event: {} happened to picture: {}", kind, file);
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(file)) {
				logger.info("Detected new Picture directory: {}", file);
				watcherService.walkTreeAndSetWatches(
						file,
						path -> scanPicture(path, getExistingPictures()),
						this::handleFileEvent
				);
			} else {
				logger.info("Found new Picture: {}", file);
				scanPicture(file, new ArrayList<>());
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			// TODO: handle modify picture
			logger.info("Picture was modified: {}", file);
		} else if (ENTRY_DELETE.equals(kind)) {
			logger.info("Deleting Picture at: {}", file);
			getPictureByPath(file).forEach(pic -> deletePicture(pic.getId()));
		}
	}

	public Page<Picture> getLatestPictures(int page, int pageSize) {
		return pictureRepo.findLatest(PageRequest.of(page, pageSize));
	}

	public Page<UserImageView> getLatestUserPictures(int page, int size) {
		return getLatestPictures(page, size).map(this::getUserImageView);
	}

	public UserImageView getUserImageView(@NotNull Picture picture) {
		return userService.getUserPreferences().isFavorite(picture) ?
				UserImageView.favorite(picture) : UserImageView.of(picture);
	}

	public Collection<UserImageView> getUserImageViews(@NotNull Collection<Picture> pictures) {
		return pictures.stream().map(this::getUserImageView).toList();
	}

	public List<Picture> getRandomPictures(int count) {
		return pictureRepo.findRandom(PageRequest.ofSize(count));
	}

	public List<UserImageView> getRandomUserPictures(int count) {
		return getRandomPictures(count).stream().map(this::getUserImageView).toList();
	}

	public Optional<Picture> getPicture(@NotNull UUID picId) {
		return pictureRepo.findById(picId);
	}

	public Optional<UserImageView> getUserPicture(@NotNull UUID picId) {
		return getPicture(picId).map(this::getUserImageView);
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

	public UserImageView toggleIsPictureFavorite(@NotNull UUID picId) {
		Optional<Picture> optional = getPicture(picId);
		if (optional.isEmpty()) {
			throw new IllegalArgumentException("Trying to favorite non-existent Picture: " + picId);
		}
		Picture picture = optional.get();
		UserPreferences preferences = userService.getUserPreferences();
		if (preferences.toggleFavorite(picture)) {
			return UserImageView.favorite(picture);
		}
		return UserImageView.of(picture);
	}

	public Collection<UserImageView> getFavoritePictures() {
		return userService.getUserPreferences()
				.getFavoritePictures().stream()
				.map(this::getUserImageView)
				.toList();
	}
}
