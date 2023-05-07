package self.me.mp.model;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class SearchAllResult {

	private Collection<UserVideoView> videos;
	private Collection<UserImageView> pictures;
	private Collection<UserComicBookView> comics;

	private long totalResults;
	private int offset;
	private int limit;

}
