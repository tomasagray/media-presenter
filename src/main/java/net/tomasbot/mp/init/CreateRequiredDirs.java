package net.tomasbot.mp.init;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class CreateRequiredDirs implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(CreateRequiredDirs.class);

  @Value(
      "#{'${pictures.location}, ${comics.location}, ${videos.add-location}, ${videos.storage-location},"
          + " ${videos.convert-location}, ${video.thumbnails.location}'.split(', ')}")
  private List<Path> required = new ArrayList<>();

  @Override
  public void run(String... args) throws Exception {
    logger.info("Ensuring existence of required directories...");

    for (Path directory : required) {
      File directoryFile = directory.toFile();
      if (!directoryFile.exists()) {
        logger.info("Required directory: {} does not exist; creating...", directory);
        boolean created = directoryFile.mkdirs();
        if (!created && !directoryFile.exists()) {
          throw new IOException("Could not create required directory: " + directory);
        }
        logger.info("Directory: {} was created.", directory);
      } else {
        logger.info("Required directory: {} was found", directory);
      }
    }
  }
}
