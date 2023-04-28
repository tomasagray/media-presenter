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
import self.me.mp.api.controller.ComicBookController;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Image;
import self.me.mp.model.Tag;

import java.sql.Timestamp;
import java.util.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "comic")
@Relation(collectionRelation = "comics")
public class ComicBookResource extends RepresentationModel<ComicBookResource> {

	private UUID id;
	private String title;
	private Set<Tag> tags;
	private Timestamp timestamp;

	@Component
	public static class ComicBookModeller extends RepresentationModelAssemblerSupport<ComicBook, ComicBookResource> {

		public ComicBookModeller() {
			super(ComicBookController.class, ComicBookResource.class);
		}

		@Override
		public @NotNull ComicBookResource toModel(@NotNull ComicBook entity) {
			ComicBookResource model = instantiateModel(entity);
			model.setId(entity.getId());
			model.setTitle(entity.getTitle());
			model.setTimestamp(entity.getAdded());
			model.setTags(entity.getTags());
			// links
			List<Image> images = new ArrayList<>(entity.getImages());
			images.sort(Comparator.comparing(Image::getUri));
			Image cover = images.get(0);
			if (cover != null) {
				model.add(
						linkTo(methodOn(ComicBookController.class)
								.getPageData(cover.getId()))
								.withRel("data"));
			}

			System.out.printf("Comic: %s has %d pages%n", entity.getId(), entity.getImages().size());

			for (int i = 0; i < images.size(); i++) {
				model.add(
						linkTo(methodOn(ComicBookController.class)
								.getPageData(images.get(i).getId()))
								.withRel("page_" + i)
				);
			}
			return model;
		}
	}
}
