package self.me.mp.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

  private static final int STARTUP_TASKS = 5;
  private static final int WATCH_THREADS = 5;
  private static final int WATCH_QUEUE_CAPACITY = 1_000;
  private static final int SCAN_TASKS = 50;
  private static final int MAX_SCAN_TASKS = 125;
  private static final int TRANSCODE_TASKS = 4;
  private static final int TRANSCODE_QUEUE_SIZE = 1_000;

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
    executor.setCorePoolSize(TRANSCODE_TASKS);
    executor.setMaxPoolSize(TRANSCODE_QUEUE_SIZE);
    executor.setQueueCapacity(Integer.MAX_VALUE);
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
