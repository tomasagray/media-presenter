package self.me.mp.plugin.ffmpeg;

import lombok.Getter;
import lombok.Setter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public abstract class LoggableThread extends Thread {

	@Getter
	@Setter
	protected Process process;

	@Getter
	@Setter
	protected boolean loggingEnabled = true;

	@Getter
	@Setter
	protected Flux<String> logPublisher;

	public Disposable onLoggableEvent(Consumer<? super String> fn) {
		if (logPublisher != null) {
			return logPublisher.subscribe(fn);
		}
		return Flux.empty().subscribe();
	}
}
