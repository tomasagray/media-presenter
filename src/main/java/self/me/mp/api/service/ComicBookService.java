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
import self.me.mp.db.ComicBookRepository;
import self.me.mp.db.ImageRepository;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Image;
import self.me.mp.model.Tag;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class ComicBookService {

	private static final Logger logger = LogManager.getLogger(ComicBookService.class);
	private final ComicBookRepository comicBookRepo;
	private final ImageRepository imageRepository;
	private final RecursiveWatcherService watcherService;
	private final TagService tagService;
	private final Map<String, Path> invalidFiles = new HashMap<>();
	@Value("${comics.location}")
	private Path comicsLocation;

	public ComicBookService(
			ComicBookRepository comicBookRepo,
			ImageRepository imageRepository,
			RecursiveWatcherService watcherService,
			TagService tagService) {
		this.comicBookRepo = comicBookRepo;
		this.imageRepository = imageRepository;
		this.watcherService = watcherService;
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

	public void init() throws IOException {
		logger.info("Scanning Comic Books in: {}", comicsLocation);
		watcherService.watch(
				comicsLocation,
				this::scanComicBook,
				this::handleFileEvent
		);
	}

	private synchronized void scanComicBook(@NotNull Path file) {
		try {
			logger.info("Found Comic Book page: {}", file);
			List<ComicBook> existing = comicBookRepo.findAll();
			Path parent = file.getParent();
			if (!isPathWithin(comicsLocation, parent)) {
				throw new UncheckedIOException(new IOException("Path is not within a Comic Book: " + file));
			}
			Optional<ComicBook> comicOpt = existing.stream()
					.filter(comic -> comic.getLocation().equals(parent))
					.findFirst();
			if (comicOpt.isPresent()) {
				ComicBook comic = comicOpt.get();
				addPageToComic(file, comic);
			} else {
				ComicBook comic = createComic(file);
				logger.info("Created new Comic Book: {}", comic);
			}
		} catch (Throwable e) {
			logger.error("File could not be added to Comic: {}; {}", file, e.getMessage(), e);
			String ext = FilenameUtils.getExtension(file.toString());
			invalidFiles.put(ext, file);
		}
	}

	private void addPageToComic(@NotNull Path file, @NotNull ComicBook comic)
			throws IOException {

		logger.info("Adding page: {} to Comic Book: {}", file, comic);
		Optional<Image> imgOpt = comic.getImages()
				.stream()
				.filter(img -> img.getUri().equals(file.toUri()))
				.findFirst();
		if (imgOpt.isEmpty()) {
			Image img = createImage(file);
			comic.addImage(img);
			save(comic);
		} else {
			logger.info("Page: {} is already in Comic Book: {}", file, comic);
		}
	}

	private ComicBook createComic(@NotNull Path file) throws IOException {
		Path parent = file.getParent();
		LinkedList<String> names = getComicNames(parent);
		String comicName = names.removeLast();
		Set<Image> images = new HashSet<>();
		images.add(createImage(file));
		List<Tag> tags = names.stream()
				.map(tagService::getOrCreateTag)
				.toList();
		ComicBook comic = ComicBook.builder()
				.location(parent)
				.title(comicName)
				.images(images)
				.tags(new HashSet<>(tags))  // ensure mutable
				.build();
		return save(comic);
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

	public boolean isPathWithin(@NotNull Path parent, @NotNull Path child) {
		Path absParent = parent.toAbsolutePath();
		Path absChild = child.toAbsolutePath();
		return absChild.startsWith(absParent) && !absChild.equals(absParent);
	}

	private void handleFileEvent(@NotNull Path file, WatchEvent.Kind<?> kind) {
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(file)) {
				logger.info("Detected new Comic Book: {}", file);
				watcherService.walkTreeAndSetWatches(
						file,
						this::scanComicBook,
						this::handleFileEvent
				);
			} else {
				logger.info("Found new Comic Book page: {}", file);
				scanComicBook(file);
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			// TODO: handle modify comic...
			logger.info("Comic Book image: {} was modified", file);
		} else if (ENTRY_DELETE.equals(kind)) {
			logger.info("Comic Book image was deleted: {}", file);
			handleDeletedImage(file);
		}
	}

	private void handleDeletedImage(@NotNull Path file) {
		Optional<Image> imgOpt = imageRepository.findByUri(file.toUri());
		if (imgOpt.isPresent()) {
			Image image = imgOpt.get();
			Optional<ComicBook> comicOpt = comicBookRepo.findComicBookByImagesContaining(image);
			if (comicOpt.isPresent()) {
				ComicBook comicBook = comicOpt.get();
				boolean removed = comicBook.getImages().remove(image);
				if (!removed) {
					throw new IllegalStateException("Could not remove Image from Comic Book: " + image);
				}
				if (comicBook.getImages().isEmpty()) {
					delete(comicBook);
				} else {
					save(comicBook);
				}
			} else {
				throw new IllegalStateException("Image deleted was not part of a Comic Book: " + image);
			}
		} else {
			throw new IllegalStateException("Detected deletion of unknown Comic Book Image: " + file);
		}
	}

	public List<ComicBook> getAllComics() {
		return comicBookRepo.findAll();
	}

	public Page<ComicBook> getAllComics(int page, int size) {
		return comicBookRepo.findAll(PageRequest.of(page, size));
	}

	public Page<ComicBook> getLatestComics(int page, int size) {
		return comicBookRepo.findLatest(PageRequest.of(page, size));
	}

	public Optional<ComicBook> getComicBook(UUID bookId) {
		return comicBookRepo.findById(bookId);
	}

	public Optional<UrlResource> getPageData(@NotNull UUID pageId) {
		return imageRepository.findById(pageId)
				.map(image -> UrlResource.from(image.getUri()));
	}

	public Map<String, Path> getInvalidFiles() {
		return invalidFiles;
	}

	public ComicBook save(@NotNull ComicBook comicBook) {
		return comicBookRepo.save(comicBook);
	}

	public void delete(@NotNull ComicBook comicBook) {
		comicBookRepo.delete(comicBook);
	}
}