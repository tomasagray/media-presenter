package self.me.mp.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * JSON parsing wrapper
 */
public final class JsonParser {

	private static final Gson gson = new GsonBuilder().create();

	public static <T> T fromJson(@NotNull String json, @NotNull Class<T> clazz) {
		return gson.fromJson(json, clazz);
	}

	public static String toJson(@NotNull Object data) {
		return gson.toJson(data);
	}
}
