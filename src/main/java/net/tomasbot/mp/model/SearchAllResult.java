package net.tomasbot.mp.model;

import lombok.Builder;
import lombok.Data;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserImageView;
import net.tomasbot.mp.user.UserVideoView;

@Data
@Builder
public class SearchAllResult {

  private SearchResults<UserVideoView> videos;
  private SearchResults<UserImageView> pictures;
  private SearchResults<UserComicBookView> comics;

  private long totalResults;
  private int offset;
  private int limit;

  public boolean isEmpty() {
    return videos.isEmpty() && pictures.isEmpty() && comics.isEmpty();
  }
}
