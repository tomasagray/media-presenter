package net.tomasbot.mp.api.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.tomasbot.mp.model.Editable;
import net.tomasbot.mp.model.Tag;
import net.tomasbot.mp.util.JsonParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecyclingService {

  private static final Logger logger = LogManager.getLogger(RecyclingService.class);

  @Value("${application.config.recycle-bin-location}")
  private Path recycleBinLocation;

  private static void createMetadataFile(@NonNull Path recycleLocation, @NotNull RecycledEntityMetadata metadata)
          throws IOException {
    Path fileName = recycleLocation.getFileName();

    String baseName = FilenameUtils.getBaseName(fileName.toString());
    String ext = FilenameUtils.getExtension(fileName.toString());
    String metadataLocation = String.format("%s._%s_.metadata.json", baseName, ext);
    Path metaFile = recycleLocation.resolveSibling(metadataLocation);

    String metaJson = JsonParser.toJson(metadata);
    Files.writeString(metaFile, metaJson);
  }

  public void recycle(@NonNull Editable entity) throws IOException {
    Path location = entity.getLocation();

    Path recycleLocation = recycleBinLocation.resolve(location.getFileName());
    // get metadata from the entity
    RecycledEntityMetadata metadata = RecycledEntityMetadata.from(entity);
    createMetadataFile(recycleLocation, metadata);

    // move to recycle bin
    logger.info("Moving {} to recycle bin at {}", location, recycleLocation);
    boolean renamed = location.toFile().renameTo(recycleLocation.toFile());
    if (!renamed) throw new IllegalStateException("Failed to move file to recycle bin: " + location);
  }

  @Data
  @AllArgsConstructor
  static class RecycledEntityMetadata {
    private UUID id;
    private String title;
    private Path originalLocation;
    private Set<String> tags;

    @Contract("_ -> new")
    public static @NonNull RecycledEntityMetadata from(@NonNull Editable entity) {
      UUID id = entity.getId();
      String title = entity.getTitle();
      Path location = entity.getLocation();
      // process Tags
      Set<String> tags = entity.getTags()
              .stream()
              .map(Tag::getName)
              .collect(Collectors.toSet());

      return new RecycledEntityMetadata(id, title, location, tags);
    }
  }
}
