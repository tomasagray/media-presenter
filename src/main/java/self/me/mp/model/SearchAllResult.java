package self.me.mp.model;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class SearchAllResult {

	private Collection<Video> videos;
	private Collection<Picture> pictures;
	private Collection<ComicBook> comics;

	private long totalResults;
	private int offset;
	private int limit;

}
