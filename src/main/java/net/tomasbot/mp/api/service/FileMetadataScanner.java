package net.tomasbot.mp.api.service;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface FileMetadataScanner<T> {

  void scanFileMetadata(@NotNull T t) throws Throwable;
}
