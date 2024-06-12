package net.tomasbot.mp.util;

import com.google.gson.*;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/** JSON parsing wrapper */
public final class JsonParser {

  private static final Gson gson =
      new GsonBuilder()
          .registerTypeHierarchyAdapter(
              Path.class,
              (JsonDeserializer<Path>)
                  (json, type, context) -> {
                    final String data = json.getAsJsonPrimitive().getAsString();
                    return Path.of(data);
                  })
          .registerTypeHierarchyAdapter(
              Path.class,
              (JsonSerializer<Path>)
                  (path, type, context) -> {
                    if (path == null) {
                      return null;
                    }
                    return new JsonPrimitive(path.toString());
                  })
          .create();

  public static <T> T fromJson(@NotNull String json, @NotNull Class<T> clazz) {
    return gson.fromJson(json, clazz);
  }

  public static String toJson(@NotNull Object data) {
    return gson.toJson(data);
  }
}
