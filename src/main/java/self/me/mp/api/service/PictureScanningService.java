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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@Service
public class PictureScanningService implements FileScanningService<Picture> {

	private static final Logger logger = LogManager.getLogger(PictureScanningService.class);

	private static final MultiValueMap<String, Path> invalidFiles = new LinkedMultiValueMap<>();

	private final TagService tagService;

	@Value("${pictures.location}")
	private Path pictureLocation;

	public PictureScanningService(TagService tagService) {
		this.tagService = tagService;
	}

	@Async("fileScanner")
	@Override
	public void scanFile(
			@NotNull Path file,
			@NotNull Collection<Picture> existing,
			@NotNull Consumer<Picture> onSave) {

		try {
			List<Path> existingPaths =
					existing.stream().map(Picture::getUri).map(Paths::get).toList();
			if (!existingPaths.contains(file)) {
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
					onSave.accept(picture);
				} else {
					throw new IOException("Image data was null");
				}
			}
		} catch (Throwable e) {
			logger.error("Found invalid Picture file: {}", file);
			String ext = FilenameUtils.getExtension(file.toString());
			invalidFiles.add(ext, file);
		}
	}

	@Override
	public MultiValueMap<String, Path> getInvalidFiles() {
		return invalidFiles;
	}
}
