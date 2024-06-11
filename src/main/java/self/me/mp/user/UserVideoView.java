package self.me.mp.user;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.mp.model.ImageSet;
import self.me.mp.model.Tag;
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegFormat;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserVideoView extends UserView {

    private UUID id;
    private String title;
    private ImageSet thumbnails;
    private Collection<Tag> tags;
    private Timestamp timestamp;
    private double duration;

    @Component
    public static class UserVideoModeller extends UserViewModeller<Video, UserVideoView> {

    private static double getDuration(@NotNull Video data) {
      FFmpegMetadata metadata = data.getMetadata();
      if (metadata == null) return 0;
      FFmpegFormat format = metadata.getFormat();
      if (format == null) return 0;
      return format.getDuration();
    }

        @Override
        public UserVideoView toView(@NotNull Video data) {
            UserVideoView view = new UserVideoView();
            view.setId(data.getId());
            view.setTitle(data.getTitle());
            view.setThumbnails(data.getThumbnails());
            view.setTags(data.getTags());
            view.setTimestamp(data.getAdded());
            view.setDuration(getDuration(data));
            return view;
        }
    }
}
