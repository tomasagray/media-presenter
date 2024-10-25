package net.tomasbot.mp.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.UUID;
import lombok.*;
import net.tomasbot.mp.api.controller.VideoController;
import net.tomasbot.mp.model.ImageSet;
import net.tomasbot.mp.user.UserVideoView;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video")
@Relation(collectionRelation = "videos")
public class VideoResource extends EntityResource<VideoResource> {

  private Timestamp timestamp;
  private String duration;

  @Component
  public static class VideoResourceModeller
      extends RepresentationModelAssemblerSupport<UserVideoView, VideoResource> {

    public VideoResourceModeller() {
      super(VideoController.class, VideoResource.class);
    }

    private @NotNull String getDuration(@NotNull UserVideoView video) {
      long millis = (long) (video.getDuration() * 1_000);
      Duration duration = Duration.ofMillis(millis);
      int hours = duration.toHoursPart();
      String hoursPart = hours > 0 ? String.format("%02d:", hours) : "";
      return hoursPart
          + String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());
    }

    @SneakyThrows
    @Override
    public @NotNull VideoResource toModel(@NotNull UserVideoView entity) {

      VideoResource model = instantiateModel(entity);
      UUID videoId = entity.getId();

      model.setId(videoId);
      model.setTitle(entity.getTitle());
      model.setTimestamp(entity.getTimestamp());
      model.setTags(entity.getTags());
      model.setFavorite(entity.isFavorite());
      model.setDuration(getDuration(entity));

      // links
      model.add(
          linkTo(methodOn(VideoController.class).getVideoData(videoId, new HttpHeaders()))
              .withRel(DATA_REL));
      model.add(linkTo(methodOn(VideoController.class).updateVideo(model)).withRel(UPDATE_REL));
      model.add(
          linkTo(methodOn(VideoController.class).toggleVideoFavorite(videoId)).withRel(FAVORITE_REL));
      // add thumb links
      ImageSet thumbnails = entity.getThumbnails();
      if (thumbnails != null) {
        thumbnails
            .getImages()
            .forEach(
                img ->
                    model.add(
                        linkTo(methodOn(VideoController.class).getThumbnail(videoId, img.getId()))
                            .withRel(THUMBNAIL_REL)));
      }

      return model;
    }

    public UserVideoView fromModel(@NotNull VideoResource resource) {
      UserVideoView userVideoView = new UserVideoView();
      userVideoView.setId(resource.getId());
      userVideoView.setTitle(resource.getTitle());
      userVideoView.setTags(resource.getTags());
      userVideoView.setFavorite(resource.isFavorite());
      return userVideoView;
    }
  }
}
