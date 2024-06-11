package self.me.mp.model;

import java.util.Collection;
import lombok.Builder;
import lombok.Data;
import self.me.mp.user.UserComicBookView;
import self.me.mp.user.UserImageView;
import self.me.mp.user.UserVideoView;

@Data
@Builder
public class SearchAllResult {

  private Collection<UserVideoView> videos;
  private Collection<UserImageView> pictures;
  private Collection<UserComicBookView> comics;

  private long totalResults;
  private int offset;
  private int limit;

  public boolean isEmpty() {
    return videos.isEmpty() && pictures.isEmpty() && comics.isEmpty();
  }
}
