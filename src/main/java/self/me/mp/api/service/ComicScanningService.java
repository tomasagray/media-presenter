package self.me.mp.api.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Image;
import self.me.mp.model.Tag;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

@Service
public class ComicScanningService implements FileScanningService<ComicBook> {

	private static final Logger logger = LogManager.getLogger(ComicScanningService.class);
	private static final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	private final TagService tagService;

	@Value("${comics.location}")
	private Path comicsLocation;

	public ComicScanningService(TagService tagService) {
		this.tagService = tagService;
	}

	private static Image createImage(@NotNull Path file) throws IOException {
		File input = file.toFile();
		BufferedImage image = ImageIO.read(input);
		return Image.builder()
				.width(image.getWidth())
				.height(image.getHeight())
				.filesize(input.length())
				.title(FilenameUtils.getBaseName(file.toString()))
				.uri(file.toUri())
				.build();
	}

	@Override
	public void scanFile(
			@NotNull Path file,
			@NotNull Collection<ComicBook> existing,
			@NotNull Consumer<ComicBook> onSave) {

		try {
			logger.info("Found Comic Book page: {}", file);
			Path parent = file.getParent();
			if (!isPathWithin(comicsLocation, parent)) {
				throw new UncheckedIOException(new IOException("Path is not within a Comic Book: " + file));
			}
			Optional<ComicBook> comicOpt = existing.stream()
					.filter(comic -> comic.getLocation().equals(parent))
					.findFirst();
			if (comicOpt.isPresent()) {
				ComicBook comic = comicOpt.get();
				addPageToComic(file, comic, onSave);
			} else {
				ComicBook comic = createComic(file, onSave);
				logger.info("Created new Comic Book: {}", comic);
			}
		} catch (Throwable e) {
			logger.error("File could not be added to Comic: {}; {}", file, e.getMessage(), e);
			String ext = FilenameUtils.getExtension(file.toString());
			invalidFiles.add(ext, file);
		}
	}

	private boolean isPathWithin(@NotNull Path parent, @NotNull Path child) {
		Path absParent = parent.toAbsolutePath();
		Path absChild = child.toAbsolutePath();
		return absChild.startsWith(absParent) && !absChild.equals(absParent);
	}

	private ComicBook createComic(@NotNull Path file, @NotNull Consumer<ComicBook> onSave)
			throws IOException {
		Path parent = file.getParent();
		LinkedList<String> names = getComicNames(parent);
		String comicName = names.removeLast();
		Set<Image> images = new HashSet<>();
		images.add(createImage(file));
		List<Tag> tags = tagService.getTags(comicsLocation.relativize(file));
		ComicBook comic = ComicBook.builder()
				.location(parent)
				.title(comicName)
				.images(images)
				.tags(new HashSet<>(tags))  // ensure mutable
				.build();
		onSave.accept(comic);
		return comic;
	}

	private void addPageToComic(
			@NotNull Path file,
			@NotNull ComicBook comic,
			@NotNull Consumer<ComicBook> onSave)
			throws IOException {

		logger.info("Adding page: {} to Comic Book: {}", file, comic);
		Optional<Image> imgOpt = comic.getImages()
				.stream()
				.filter(img -> img.getUri().equals(file.toUri()))
				.findFirst();
		if (imgOpt.isEmpty()) {
			Image img = createImage(file);
			comic.addImage(img);
			onSave.accept(comic);
		} else {
			logger.info("Page: {} is already in Comic Book: {}", file, comic);
		}
	}

	@NotNull
	private LinkedList<String> getComicNames(Path parent) {
		Path relativized = comicsLocation.relativize(parent);
		LinkedList<String> names = new LinkedList<>();
		for (int i = 0; i < relativized.getNameCount(); i++) {
			names.add(relativized.getName(i).toString());
		}
		return names;
	}

	@Override
	public MultiValueMap<String, Path> getInvalidFiles() {
		return invalidFiles;
	}
}
