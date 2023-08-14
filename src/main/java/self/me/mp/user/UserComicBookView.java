package self.me.mp.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Image;
import self.me.mp.model.Tag;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserComicBookView extends UserView {

	private UUID id;
	private String title;
	private Timestamp timestamp;
	private Collection<Image> images;
	private Collection<Tag> tags;

	@Component
	public static class UserComicModeller extends UserViewModeller<ComicBook, UserComicBookView> {

		@Override
		public UserComicBookView toView(@NotNull ComicBook data) {
			UserComicBookView view = new UserComicBookView();
			view.setId(data.getId());
			view.setTitle(data.getTitle());
			view.setTimestamp(data.getAdded());
			view.setImages(data.getImages());
			view.setTags(data.getTags());
			return view;
		}
	}
}
