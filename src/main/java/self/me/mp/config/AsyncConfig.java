package self.me.mp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

	private static final int WATCH_THREADS = 5;
	private static final int WATCH_QUEUE_CAPACITY = 1000;
	private static final int SCAN_TASKS = 9;
	private static final int MAX_SCAN_TASKS = 12;

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
}
