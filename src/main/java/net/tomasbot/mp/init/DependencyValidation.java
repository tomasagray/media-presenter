package net.tomasbot.mp.init;

import net.tomasbot.mp.plugin.ffmpeg.FFmpegPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class DependencyValidation implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(DependencyValidation.class);

  private final FFmpegPlugin ffmpeg;

  @Value("${CONFIG_ROOT}")
  private String configRoot;

  @Value("${DATA_ROOT}")
  private String dataRoot;

  @Value("${DB_URL}")
  private String dbUrl;

  @Value("${DB_USER}")
  private String dbUser;

  @Value("${DB_PASSWORD}")
  private String dbPassword;

  @Value("${REMEMBER_ME_KEY}")
  private String rememberMeKey;

  public DependencyValidation(FFmpegPlugin ffmpeg) {
    this.ffmpeg = ffmpeg;
  }

  @Override
  public void run(String... args) {
    try {
      String ffmpegVersion = ffmpeg.getVersion();
      logger.info("FFMPEG version: {}", ffmpegVersion);

      validateLocations();
      logger.info("Startup dependency validation finished successfully");
    } catch (Throwable e) {
      logger.error("Startup dependency validation failed!", e);
      System.exit(-1);
    }
  }


  private void validateLocations() {
    List.of(dataRoot, configRoot, dbUrl, dbUser, dbPassword, rememberMeKey)
            .forEach(location -> {
              if (location == null || location.isEmpty())
                throw new IllegalStateException("Missing required environment variable - recheck configuration!");
            });
  }
}
