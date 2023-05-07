package self.me.mp.model;

import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

@Data
public class UserVideoView {

	private final UUID id;
	private final String title;
	private final ImageSet thumbnails;
	private final Collection<Tag> tags;
	private final Timestamp timestamp;
	private boolean isFavorite;
	UserVideoView(@NotNull Video video) {
		this.id = video.getId();
		this.title = video.getTitle();
		this.thumbnails = video.getThumbnails();
		this.tags = video.getTags();
		this.timestamp = video.getAdded();
	}

	@Contract("_ -> new")
	public static @NotNull UserVideoView of(@NotNull Video video) {
		return new UserVideoView(video);
	}

	public static @NotNull UserVideoView favorite(@NotNull Video video) {
		UserVideoView view = new UserVideoView(video);
		view.setFavorite(true);
		return view;
	}
}
