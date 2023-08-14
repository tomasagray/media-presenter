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
import self.me.mp.db.ComicBookRepository;
import self.me.mp.db.ImageRepository;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Image;

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
public class ComicBookService {

	private static final Logger logger = LogManager.getLogger(ComicBookService.class);
	private final ComicBookRepository comicBookRepo;
	private final FileScanningService<ComicBook> scanningService;
	private final ImageRepository imageRepository;
	private final RecursiveWatcherService watcherService;

	@Value("${comics.location}")
	private Path comicsLocation;

	public ComicBookService(
			ComicBookRepository comicBookRepo,
			ImageRepository imageRepository,
			RecursiveWatcherService watcherService,
			ComicScanningService scanningService) {
		this.comicBookRepo = comicBookRepo;
		this.imageRepository = imageRepository;
		this.watcherService = watcherService;
		this.scanningService = scanningService;
	}

	@Async("watcher")
	public void init() throws IOException {
		initializeComicBookLocation();
		logger.info("Scanning Comic Books in: {}", comicsLocation);
		List<ComicBook> existing = comicBookRepo.findAll();
		watcherService.watch(
				comicsLocation,
				file -> scanningService.scanFile(file, existing, this::save),
				this::handleFileEvent
		);
	}

	private void initializeComicBookLocation() throws IOException {
		File file = comicsLocation.toFile();
		if (!file.exists()) {
			logger.info("Comic Book storage location: {} does not exist; creating...", comicsLocation);
			if (!file.mkdirs()) {
				throw new IOException("Could not create location for Comic Book storage: " + comicsLocation);
			}
		}
	}

	private void handleFileEvent(@NotNull Path file, WatchEvent.Kind<?> kind) {
		if (ENTRY_CREATE.equals(kind)) {
			if (Files.isDirectory(file)) {
				logger.info("Detected new Comic Book: {}", file);
				watcherService.walkTreeAndSetWatches(
						file,
						page -> scanningService.scanFile(page, new ArrayList<>(), this::save),
						null,
						this::handleFileEvent
				);
			} else {
				logger.info("Found new Comic Book page: {}", file);
				List<ComicBook> existing = comicBookRepo.findAll();
				scanningService.scanFile(file, existing, this::save);
			}
		} else if (ENTRY_MODIFY.equals(kind)) {
			// TODO: handle modify comic...
			logger.info("Comic Book image: {} was modified", file);
		} else if (ENTRY_DELETE.equals(kind)) {
			logger.info("Comic Book image was deleted: {}", file);
			String ext = FilenameUtils.getExtension(file.toString());
			List<Path> invalidPaths = scanningService.getInvalidFiles().get(ext);
			if (invalidPaths != null) {
				boolean removed = invalidPaths.remove(file);
				if (removed) {
					logger.info("Deleted invalid file: {}", file);
					return;
				}
			}
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

	public Page<ComicBook> getAllComics(int page, int size) {
		return comicBookRepo.findAll(PageRequest.of(page, size));
	}

	public Page<ComicBook> getLatestComics(int page, int size) {
		return comicBookRepo.findLatest(PageRequest.of(page, size));
	}

	public List<ComicBook> getRandomComics(int count) {
		return comicBookRepo.findRandomComics(PageRequest.ofSize(count));
	}

	public Optional<ComicBook> getComicBook(UUID bookId) {
		return comicBookRepo.findById(bookId);
	}

	public Optional<UrlResource> getPageData(@NotNull UUID pageId) {
		return imageRepository.findById(pageId)
				.map(image -> UrlResource.from(image.getUri()));
	}

	public MultiValueMap<String, Path> getInvalidFiles() {
		return scanningService.getInvalidFiles();
	}

	public ComicBook save(@NotNull ComicBook comicBook) {
		return comicBookRepo.save(comicBook);
	}

	public void delete(@NotNull ComicBook comicBook) {
		comicBookRepo.delete(comicBook);
	}
}
