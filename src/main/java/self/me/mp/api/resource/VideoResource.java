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
import self.me.mp.model.Video;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.sql.Timestamp;
import java.util.Set;
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
	private Set<Tag> tags;
	private FFmpegMetadata metadata;

	@Component
	public static class VideoResourceModeller
			extends RepresentationModelAssemblerSupport<Video, VideoResource> {

		public VideoResourceModeller() {
			super(VideoController.class, VideoResource.class);
		}

		@SneakyThrows
		@Override
		public @NotNull VideoResource toModel(@NotNull Video entity) {
			VideoResource model = instantiateModel(entity);
			model.setId(entity.getId());
			model.setTitle(entity.getTitle());
			model.setTimestamp(entity.getAdded());
			model.setTags(entity.getTags());
			model.setMetadata(entity.getMetadata());
			model.add(
					linkTo(methodOn(VideoController.class)
							.getVideoData(entity.getId(), new HttpHeaders()))
							.withRel("data"));
			// TODO: add link to thumbs
			return model;
		}
	}
}
