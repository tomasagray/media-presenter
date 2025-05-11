package net.tomasbot.mp.api.service;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import net.tomasbot.mp.model.ComicPage;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.Video;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.file.Path;
import java.util.Collection;

@Service
public class InvalidFilesService {

  private final Multimap<String, Path> invalidVideoFiles = TreeMultimap.create();
  private final Multimap<String, Path> invalidPictureFiles = TreeMultimap.create();
  private final Multimap<String, Path> invalidComicBookFiles = TreeMultimap.create();

  private static void addInvalidFile(
          @NotNull Path file, @NotNull Multimap<String, Path> invalidFiles) {
    final String extension = FilenameUtils.getExtension(file.toString());
    invalidFiles.put(extension, file);
  }

  private static boolean removeInvalidFile(
          @NotNull Path file, @NotNull Multimap<String, Path> invalidFiles) {
    final String extension = FilenameUtils.getExtension(file.toString());
    Collection<Path> paths = invalidFiles.get(extension);

    if (!paths.isEmpty()) {
      return invalidFiles.remove(extension, file);
    }

    return false;
  }

  @NotNull
  private static MultiValueMap<String, Path> getExportableInvalidFiles(
          @NotNull Multimap<String, Path> invalidFiles) {
    MultiValueMap<String, Path> exportable = new LinkedMultiValueMap<>();

    for (String ext : invalidFiles.keySet()) {
      exportable.put(ext, invalidFiles.get(ext).stream().toList());
    }

    return exportable;
  }

  /**
   * Add a file to the list of invalid files
   *
   * @param file The path of the invalid file
   * @param type Must be one of: Video, Picture or ComicPage
   */
  public void addInvalidFile(@NotNull Path file, @NotNull Class<?> type) {
    if (Video.class.equals(type)) {
      addInvalidFile(file, invalidVideoFiles);
    } else if (Picture.class.equals(type)) {
      addInvalidFile(file, invalidPictureFiles);
    } else if (ComicPage.class.equals(type)) {
      addInvalidFile(file, invalidComicBookFiles);
    } else throw new IllegalArgumentException("Unknown entity type: " + type);
  }

  /**
   * Remove a file from the list of invalid files
   *
   * @param file The path of the invalid file
   * @param type Must be one of: Video, Picture or ComicPage
   */
  public boolean deleteInvalidFile(@NotNull Path file, @NotNull Class<?> type) {
    if (Video.class.equals(type)) {
      return removeInvalidFile(file, invalidVideoFiles);
    } else if (Picture.class.equals(type)) {
      return removeInvalidFile(file, invalidPictureFiles);
    } else if (ComicPage.class.equals(type)) {
      return removeInvalidFile(file, invalidComicBookFiles);
    } else throw new IllegalArgumentException("Unknown entity type: " + type);
  }

  public MultiValueMap<String, Path> getInvalidVideoFiles() {
    return getExportableInvalidFiles(invalidVideoFiles);
  }

  public MultiValueMap<String, Path> getInvalidPictureFiles() {
    return getExportableInvalidFiles(invalidPictureFiles);
  }

  public MultiValueMap<String, Path> getInvalidComicBookFiles() {
    return getExportableInvalidFiles(invalidComicBookFiles);
  }
}
