package self.me.mp.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.mp.model.ImageSet;
import self.me.mp.model.Tag;
import self.me.mp.model.Video;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserVideoView extends UserView {

	private UUID id;
	private String title;
	private ImageSet thumbnails;
	private Collection<Tag> tags;
	private Timestamp timestamp;

	@Component
	public static class UserVideoModeller extends UserViewModeller<Video, UserVideoView> {

		@Override
		public UserVideoView toView(@NotNull Video data) {
			UserVideoView view = new UserVideoView();
			view.setId(data.getId());
			view.setTitle(data.getTitle());
			view.setThumbnails(data.getThumbnails());
			view.setTags(data.getTags());
			view.setTimestamp(data.getAdded());
			return view;
		}
	}
}
