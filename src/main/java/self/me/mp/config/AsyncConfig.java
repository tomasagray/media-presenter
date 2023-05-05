package self.me.mp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

	private static final int CORE_POOL_SIZE = 5;
	private static final int QUEUE_CAPACITY = 1000;

	@Bean(name = "watcher")
	public Executor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(CORE_POOL_SIZE);
		executor.setMaxPoolSize(Integer.MAX_VALUE);
		executor.setQueueCapacity(QUEUE_CAPACITY);
		return executor;
	}
}
