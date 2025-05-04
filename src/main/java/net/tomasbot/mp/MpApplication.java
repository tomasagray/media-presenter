package net.tomasbot.mp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableLoadTimeWeaving
@EnableAsync(proxyTargetClass = true)
public class MpApplication {

  public static void main(String[] args) {
    SpringApplication.run(MpApplication.class, args);
  }
}
