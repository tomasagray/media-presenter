package self.me.mp.api.resource;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import self.me.mp.api.controller.VideoController;
import self.me.mp.model.Tag;
import self.me.mp.user.UserVideoView;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video")
@Relation(collectionRelation = "videos")
public class VideoResource extends RepresentationModel<VideoResource> {

	private UUID id;
	private String title;
	private Timestamp timestamp;
	private Collection<Tag> tags;
	private boolean isFavorite;
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
			return hoursPart + String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());
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
					linkTo(methodOn(VideoController.class)
							.getVideoData(videoId, new HttpHeaders()))
							.withRel("data"));
			// add thumb links
			entity.getThumbnails()
					.getImages()
					.forEach(img -> model.add(
							linkTo(methodOn(VideoController.class)
									.getThumbnail(videoId, img.getId()))
									.withRel("thumbnail")));
			model.add(
					linkTo(methodOn(VideoController.class)
							.toggleVideoFavorite(videoId))
							.withRel("favorite"));
			return model;
		}
	}
}
