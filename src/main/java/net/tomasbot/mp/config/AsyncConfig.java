package net.tomasbot.mp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

  // file watchers
  private static final int WATCH_THREADS = 5;
  private static final int WATCH_QUEUE_CAPACITY = 1_000;

  // file scanning
  private static final int SCAN_TASKS = 50;
  private static final int MAX_SCAN_TASKS = 125;

  // transcoding
  private static final int TRANSCODE_TASKS = 4;
  private static final int TRANSCODE_POOL_SIZE = 4;
  private static final int TRANSCODE_QUEUE_SIZE = 100;
  private static final String TRANSCODE_PREFIX = "transcoder-";

  // startup
  private static final int STARTUP_TASKS = 5;

  @Bean(name = "watcher")
  public Executor threadPoolTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(WATCH_THREADS);
    executor.setMaxPoolSize(Integer.MAX_VALUE);
    executor.setQueueCapacity(WATCH_QUEUE_CAPACITY);
    return executor;
  }

  @Bean(name = "fileScanner")
  public Executor getFileScannerExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(SCAN_TASKS);
    executor.setMaxPoolSize(MAX_SCAN_TASKS);
    executor.setQueueCapacity(Integer.MAX_VALUE);
    return executor;
  }

  @Bean(name = "transcoder")
  public Executor getVideoTranscodeExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // configure thread pool
    executor.setThreadNamePrefix(TRANSCODE_PREFIX);
    executor.setCorePoolSize(TRANSCODE_TASKS);
    executor.setMaxPoolSize(TRANSCODE_POOL_SIZE);
    executor.setQueueCapacity(TRANSCODE_QUEUE_SIZE);

    return executor;
  }

  @Bean(name = "startup")
  public Executor getStartupExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(STARTUP_TASKS);
    executor.setMaxPoolSize(STARTUP_TASKS * 2);
    executor.setQueueCapacity(Integer.MAX_VALUE);
    return executor;
  }
}
