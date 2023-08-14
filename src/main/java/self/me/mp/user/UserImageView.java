package self.me.mp.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.mp.model.Image;
import self.me.mp.model.Tag;

import java.util.Collection;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserImageView extends UserView {

	private UUID id;
	private String title;
	private int width;
	private int height;
	private long filesize;
	private Collection<Tag> tags;

	@Component
	public static class UserImageModeller extends UserViewModeller<Image, UserImageView> {

		@Override
		public UserImageView toView(@NotNull Image data) {
			UserImageView view = new UserImageView();
			view.setId(data.getId());
			view.setTitle(data.getTitle());
			view.setWidth(data.getWidth());
			view.setHeight(data.getHeight());
			view.setFilesize(data.getFilesize());
			view.setTags(data.getTags());
			return view;
		}
	}
}
