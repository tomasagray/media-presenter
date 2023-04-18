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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import self.me.mp.db.PictureRepository;
import self.me.mp.model.Image;
import self.me.mp.model.Picture;
import self.me.mp.model.Tag;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class PictureService {

	private static final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	private static final Logger logger = LogManager.getLogger(PictureService.class);
	private final PictureRepository pictureRepo;
	private final RecursiveWatcherService watcherService;
	private final TagService tagService;

	@Value("${pictures.location}")
	private Path pictureLocation;

	public PictureService(
			PictureRepository pictureRepo,
			RecursiveWatcherService watcherService,
			TagService tagService) {
		this.pictureRepo = pictureRepo;
		this.watcherService = watcherService;
		this.tagService = tagService;
	}

	public void init() throws IOException {

		Set<Path> existing = getExistingPictures();
		watcherService.watch(
				pictureLocation,
				picture -> scanPicture(picture, existing),
				this::handleFileEvent
		);
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
				Path resolved = pictureLocation.relativize(file);
				List<Tag> tags = getTags(resolved);

				BufferedImage image = ImageIO.read(file.toFile());
				if (image != null) {
					Picture picture = Picture.pictureBuilder()
							.width(image.getWidth())
							.height(image.getHeight())
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

	private @NotNull List<Tag> getTags(@NotNull Path resolved) {
		final List<Tag> tags = new ArrayList<>();
		int names = resolved.getNameCount() - 1;    // skip filename
		for (int i = 0; i < names; i++) {
			String name = resolved.getName(i).toString();
			tagService.fetchByName(name)
					.ifPresentOrElse(
							tags::add,
							() -> tags.add(tagService.addNewTag(name))
					);
		}
		return tags;
	}

	private void handleFileEvent(@NotNull Path file, @NotNull WatchEvent.Kind<?> kind) {
		logger.info("Event: {} happened to picture: {}", kind, file);

		if (kind.equals(ENTRY_CREATE)) {
			if (Files.isDirectory(file)) {
				logger.info("It's a dir: {}", file);
				watcherService.walkTreeAndSetWatches(
						file,
						path -> scanPicture(path, getExistingPictures()),
						this::handleFileEvent
				);
			} else {
				logger.info("Found new Picture: {}", file);
				scanPicture(file, new ArrayList<>());
			}
		} else if (kind.equals(ENTRY_MODIFY)) {
			// TODO: handle modify picture
			logger.info("Picture was modified: {}", file);
		} else if (kind.equals(ENTRY_DELETE)) {
			logger.info("Deleting Picture at: {}", file);
			getPictureByPath(file).forEach(pic -> deletePicture(pic.getId()));
		}
	}

	public Page<Picture> getLatestPictures(int page, int pageSize) {
		return pictureRepo.findLatest(PageRequest.of(page, pageSize));
	}

	public Page<Picture> getRandomPictures(int count) {
		PageRequest request = PageRequest.ofSize(count);
		return pictureRepo.findRandom(request);
	}

	public Optional<Picture> getPicture(@NotNull UUID picId) {
		return pictureRepo.findById(picId);
	}

	public Optional<UrlResource> getPictureData(@NotNull UUID picId) {
		return getPicture(picId)
				.map(Image::getUri)
				.map(uri -> {
					try {
						return uri.toURL();
					} catch (MalformedURLException ignore) {
						return null;
					}
				})
				.map(UrlResource::new);
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
