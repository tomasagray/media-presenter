package net.tomasbot.mp.plugin.ffmpeg;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/** Class which creates an FFMPEG concatenation task; concatenates multiple video files into one */
@Data
@EqualsAndHashCode(callSuper = true)
public final class FFmpegConcatStreamTask extends FFmpegStreamTask {

  private static final String CONCAT_FILENAME = "concat.txt";
  private final List<URI> uris;
  private final Path dataDir;
  private Path concatFile;

  @Builder
  public FFmpegConcatStreamTask(
      @NotNull String command,
      @NotNull TranscodeRequest request,
      boolean loggingEnabled,
      List<URI> uris) {
    this.execCommand = command;
    this.request = request;
    this.dataDir = request.getTo().getParent();
    this.loggingEnabled = loggingEnabled;
    this.uris = uris;
  }

  @Override
  @Unmodifiable
  @NotNull
  protected List<String> createExecCommand() {
    final List<String> command = new ArrayList<>();
    command.add(this.execCommand);
    command.addAll(this.getInputArgs().toList());
    command.addAll(this.getArgumentList(request.getAdditionalArgs()));
    command.add(request.getTo().toString());
    return command;
  }

  private List<String> getArgumentList(@NotNull Map<String, Object> args) {
    return args.entrySet().stream()
        .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue().toString()))
        .toList();
  }

  @Override
  protected void prepareStream() throws IOException {
    // Create output directory
    Files.createDirectories(this.getDataDir());
    // Create URI list text file
    this.concatFile = createConcatFile();
  }

  @Override
  public @NotNull Stream<String> getInputArgs() {
    return Stream.of("-f", "concat", "-safe", "0", "-i", this.concatFile.toString());
  }

  /**
   * Create the text file (concat.txt) used by FFMPEG for concatenation
   *
   * @return The path of the concat.txt file
   * @throws IOException If there is an error creating or writing the file
   */
  private Path createConcatFile() throws IOException {
    // Map each URI to en entry in the concat file
    final String concatFileText =
        uris.stream().map(url -> String.format("file '%s'\n", url)).collect(Collectors.joining());
    // Write data to file
    final Path concatFilePath = Path.of(getDataDir().toAbsolutePath().toString(), CONCAT_FILENAME);
    return Files.writeString(concatFilePath, concatFileText);
  }
}
