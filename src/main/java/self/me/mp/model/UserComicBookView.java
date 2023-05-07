package self.me.mp.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

@Data
public class UserComicBookView {

	private final UUID id;
	private final String title;
	private final Timestamp timestamp;
	private final Collection<Image> images;
	private final Collection<Tag> tags;
	private boolean isFavorite;
	UserComicBookView(@NotNull ComicBook comicBook) {
		this.id = comicBook.getId();
		this.title = comicBook.getTitle();
		this.timestamp = comicBook.getAdded();
		this.images = comicBook.getImages();
		this.tags = comicBook.getTags();
	}

	public static @NotNull UserComicBookView favorite(@NotNull ComicBook comicBook) {
		UserComicBookView userComicBookView = new UserComicBookView(comicBook);
		userComicBookView.setFavorite(true);
		return userComicBookView;
	}

	public static @NotNull UserComicBookView of(@NotNull ComicBook comicBook) {
		return new UserComicBookView(comicBook);
	}
}
