package net.tomasbot.mp.api.service;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class FileTransferWatcher {

	private static final Map<Path, Thread> transferringFiles = new ConcurrentHashMap<>();
	private static final int XFER_TIMEOUT = 1_000;

	private static void startTransferWatch(@NotNull Path path, @NotNull Consumer<Path> onFinish) {
		final Thread fileWatcher = new Thread(() -> {
			try {
				TimeUnit.MILLISECONDS.sleep(XFER_TIMEOUT);
				// if we get here, we have received no messages for the duration of the timeout;
				// the file is done transferring
				onFinish.accept(path);
			} catch (InterruptedException ignore) {
				// this will happen repeatedly: as the file transfers, new WatchEvents are created
			}
		});
		transferringFiles.put(path, fileWatcher);
		fileWatcher.start();
	}

	public void watchFileTransfer(@NotNull Path path, @NotNull Consumer<Path> onFinish) {
		final Thread watcher = transferringFiles.get(path);
		if (watcher != null) {
			watcher.interrupt();
		}
		startTransferWatch(path, onFinish);
	}
}
