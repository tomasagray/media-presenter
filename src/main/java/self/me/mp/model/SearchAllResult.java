package self.me.mp.model;

import lombok.Builder;
import lombok.Data;
import self.me.mp.user.UserComicBookView;
import self.me.mp.user.UserImageView;
import self.me.mp.user.UserVideoView;

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

    public boolean isEmpty() {
        return videos.isEmpty() && pictures.isEmpty() && comics.isEmpty();
    }

}
