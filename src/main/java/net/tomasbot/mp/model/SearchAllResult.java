package net.tomasbot.mp.model;

import java.util.Collection;
import lombok.Builder;
import lombok.Data;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserImageView;
import net.tomasbot.mp.user.UserVideoView;

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
