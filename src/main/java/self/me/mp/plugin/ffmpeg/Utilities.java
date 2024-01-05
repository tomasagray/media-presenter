package self.me.mp.plugin.ffmpeg;

import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class Utilities {

	public static String getNormalizedPath(@NotNull URI uri) {
		if ("file".equals(uri.getScheme())) {
			return uri.getPath();
		}
		return uri.toString();
	}

}
