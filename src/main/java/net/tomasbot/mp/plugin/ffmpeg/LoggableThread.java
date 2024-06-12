package net.tomasbot.mp.plugin.ffmpeg;

import java.util.function.Consumer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import reactor.core.publisher.Flux;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class LoggableThread extends Thread {

	protected Process process;
	protected Flux<String> logPublisher;
	protected boolean loggingEnabled = true;
	protected Consumer<? super String> onEvent;
	protected Consumer<? super Integer> onComplete;
	protected Consumer<Throwable> onError;

	public LoggableThread onLoggableEvent(Consumer<? super String> fn) {
		this.onEvent = fn;
		return this;
	}

	public LoggableThread onComplete(Consumer<Integer> fn) {
		this.onComplete = fn;
		return this;
	}

	public LoggableThread onError(Consumer<Throwable> fn) {
		this.onError = fn;
		return this;
	}
}
