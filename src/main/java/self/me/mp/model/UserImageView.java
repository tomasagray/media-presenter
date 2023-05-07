package self.me.mp.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

@Data
public class UserImageView {

	private final UUID id;
	private final String title;
	private final int width;
	private final int height;
	private final long filesize;
	private final Collection<Tag> tags;
	private boolean isFavorite;
	UserImageView(@NotNull Image image) {
		this.id = image.getId();
		this.title = image.getTitle();
		this.width = image.getWidth();
		this.height = image.getHeight();
		this.filesize = image.getFilesize();
		this.tags = image.getTags();
	}

	public static @NotNull UserImageView of(@NotNull Image image) {
		return new UserImageView(image);
	}

	public static @NotNull UserImageView favorite(@NotNull Image image) {
		UserImageView view = new UserImageView(image);
		view.setFavorite(true);
		return view;
	}
}
