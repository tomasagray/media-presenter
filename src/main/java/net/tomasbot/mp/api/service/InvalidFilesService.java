package net.tomasbot.mp.api.service;

import java.nio.file.Path;
import java.util.List;
import lombok.Getter;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.Video;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
@Service
public class InvalidFilesService {

  private final MultiValueMap<String, Path> invalidVideoFiles = new LinkedMultiValueMap<>();
  private final MultiValueMap<String, Path> invalidPictureFiles = new LinkedMultiValueMap<>();
  private final MultiValueMap<String, Path> invalidComicBookFiles = new LinkedMultiValueMap<>();

  private static void addInvalidFile(
      @NotNull Path file, @NotNull MultiValueMap<String, Path> invalidFiles) {
    final String extension = FilenameUtils.getExtension(file.toString());
    invalidFiles.add(extension, file);
  }

  private static boolean removeInvalidFile(
      @NotNull Path file, @NotNull MultiValueMap<String, Path> invalidFiles) {
    final String extension = FilenameUtils.getExtension(file.toString());
    List<Path> paths = invalidFiles.get(extension);
    if (paths != null && !paths.isEmpty()) {
      return invalidFiles.remove(extension, paths);
    }
    return false;
  }

  public void addInvalidFile(@NotNull Path file, @NotNull Class<?> type) {
    if (Video.class.equals(type)) {
      addInvalidFile(file, invalidVideoFiles);
    } else if (Picture.class.equals(type)) {
      addInvalidFile(file, invalidPictureFiles);
    } else if (ComicPage.class.equals(type)) {
      addInvalidFile(file, invalidComicBookFiles);
    } else throw new IllegalArgumentException("Unknown entity type: " + type);
  }

  public boolean deleteInvalidFile(@NotNull Path file, @NotNull Class<?> type) {
    if (Video.class.equals(type)) {
      return removeInvalidFile(file, invalidVideoFiles);
    } else if (Picture.class.equals(type)) {
      return removeInvalidFile(file, invalidPictureFiles);
    } else if (ComicPage.class.equals(type)) {
      return removeInvalidFile(file, invalidComicBookFiles);
    } else throw new IllegalArgumentException("Unknown entity type: " + type);
  }
}
