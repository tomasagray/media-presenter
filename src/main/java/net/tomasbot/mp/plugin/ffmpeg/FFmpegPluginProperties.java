package net.tomasbot.mp.plugin.ffmpeg;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:ffmpeg.properties")
@ConfigurationProperties(prefix = "plugin.ffmpeg")
@Data
public class FFmpegPluginProperties {

  private String title;
  private String description;

  private String ffmpegLocation;
  private String ffprobeLocation;
}
