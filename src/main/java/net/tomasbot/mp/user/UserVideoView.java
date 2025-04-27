package net.tomasbot.mp.user;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.mp.model.ImageSet;
import net.tomasbot.mp.model.Tag;
import net.tomasbot.mp.model.Video;
import net.tomasbot.mp.plugin.ffmpeg.metadata.FFmpegFormat;
import net.tomasbot.mp.plugin.ffmpeg.metadata.FFmpegMetadata;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

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

    @Override
    public Video fromView(@NotNull UserVideoView data) {
      Collection<Tag> tags = data.getTags();

      final Video video = new Video();
      video.setId(data.getId());
      video.setTitle(data.getTitle());
      video.getTags().addAll(tags);
      return video;
    }
  }
}
