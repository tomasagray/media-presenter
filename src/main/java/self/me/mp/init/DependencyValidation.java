package self.me.mp.init;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import self.me.mp.plugin.ffmpeg.FFmpegPlugin;

@Component
@Order(1)
public class DependencyValidation implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(DependencyValidation.class);
  private final FFmpegPlugin ffmpeg;

  public DependencyValidation(FFmpegPlugin ffmpeg) {
    this.ffmpeg = ffmpeg;
  }

  @Override
  public void run(String... args) {
    try {
      String ffmpegVersion = ffmpeg.getVersion();
      logger.info("FFMPEG version: {}", ffmpegVersion);
    } catch (Exception e) {
      logger.error("Startup dependency validation failed!", e);
      System.exit(-1);
    }
  }
}
