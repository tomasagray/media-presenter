package self.me.mp.plugin.ffmpeg;

import lombok.Data;
import lombok.EqualsAndHashCode;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class LoggableThread extends Thread {

	protected Process process;

	protected boolean loggingEnabled = true;

	protected Flux<String> logPublisher;

	public Disposable onLoggableEvent(Consumer<? super String> fn) {
		if (logPublisher != null) {
			return logPublisher.subscribe(fn);
		}
		return Flux.empty().subscribe();
	}
}
