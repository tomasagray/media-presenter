package net.tomasbot.mp.api.service;

import java.io.IOException;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface FileMetadataScanner<T> {

  void scanFileMetadata(@NotNull T t) throws IOException;
}
