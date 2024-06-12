package net.tomasbot.mp.api.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfoService {

  @Value("${application.info.version}")
  private String appVersion;

  public ApplicationInfo getApplicationInfo() {
    Long pid = Long.parseLong(System.getProperty("PID"));
    return new ApplicationInfo(this.appVersion, pid);
  }

  @Data
  @AllArgsConstructor
  public static class ApplicationInfo {
    private String appVersion;
    private Long pid;
  }
}
