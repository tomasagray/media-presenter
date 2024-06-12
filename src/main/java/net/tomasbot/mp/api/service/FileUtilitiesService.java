package net.tomasbot.mp.api.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class FileUtilitiesService {

  private static final String ILLEGAL_FILENAME_CHAR = "�";
  private static final String ASCII_PATTERN = "\\?+";
  private static final String SAFE_CHAR = "_";

  public void repairFilename(@NotNull Path file) throws IOException {
    String filename = file.toString();
    if (filename.contains(ILLEGAL_FILENAME_CHAR)) {
      final String fixedName =
          new String(
                  Normalizer.normalize(filename, Normalizer.Form.NFKD)
                      .getBytes(StandardCharsets.US_ASCII),
                  StandardCharsets.UTF_8)
              .replaceAll(ASCII_PATTERN, SAFE_CHAR);
      File newFile = new File(fixedName);
      boolean createdDirs = newFile.mkdirs();
      boolean renamed = file.toFile().renameTo(newFile);
      final String reason =
          renamed && createdDirs
              ? "Filename contained invalid characters; renamed to: " + fixedName
              : "Could not rename file: " + file;
      // interrupt execution of scan of this file
      throw new IOException(reason);
    }
    // filename is OK; do nothing
  }
}
