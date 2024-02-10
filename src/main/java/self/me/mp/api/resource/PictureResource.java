package self.me.mp.api.resource;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.mp.api.controller.PictureController;
import self.me.mp.model.Tag;
import self.me.mp.user.UserImageView;

import java.util.Collection;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "picture")
@Relation(collectionRelation = "pictures")
public class PictureResource extends ImageResource<PictureResource> {

	private int height;
	private int width;
	private long filesize;

	@Component
	public static class PictureResourceModeller
			extends RepresentationModelAssemblerSupport<UserImageView, PictureResource> {

		public PictureResourceModeller() {
			super(PictureController.class, PictureResource.class);
		}

		@Override
		public @NotNull PictureResource toModel(@NotNull UserImageView entity) {

			PictureResource model = instantiateModel(entity);
			UUID id = entity.getId();

			model.setId(id);
			model.setTitle(entity.getTitle());
			model.setWidth(entity.getWidth());
			model.setHeight(entity.getHeight());
			model.setFilesize(entity.getFilesize());
			model.setTags(entity.getTags());
			model.setFavorite(entity.isFavorite());

			// attach links
			model.add(linkTo(methodOn(PictureController.class)
					.getPicture(id))
					.withSelfRel());
			model.add(linkTo(methodOn(PictureController.class)
					.togglePictureFavorite(id))
					.withRel("favorite"));
			model.add(linkTo(methodOn(PictureController.class)
					.getPictureData(id))
					.withRel("data"));
			return model;
		}
	}
}