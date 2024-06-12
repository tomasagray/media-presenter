package net.tomasbot.mp.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.mp.api.controller.ComicBookController;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.user.UserComicBookView;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "comic")
@Relation(collectionRelation = "comics")
public class ComicBookResource extends ImageResource<ComicBookResource> {

  private Timestamp timestamp;
  private int pageCount;

  @Component
  public static class ComicBookModeller
      extends RepresentationModelAssemblerSupport<UserComicBookView, ComicBookResource> {

    public ComicBookModeller() {
      super(ComicBookController.class, ComicBookResource.class);
    }

    @Override
    public @NotNull ComicBookResource toModel(@NotNull UserComicBookView entity) {

      ComicBookResource model = instantiateModel(entity);

      UUID comicId = entity.getId();
      model.setId(comicId);
      model.setTitle(entity.getTitle());
      model.setTimestamp(entity.getTimestamp());
      model.setTags(entity.getTags());
      model.setFavorite(entity.isFavorite());
      model.setPageCount(entity.getImages().size());

      // attach links
      model.add(linkTo(methodOn(ComicBookController.class).getComic(comicId)).withSelfRel());
      model.add(
          linkTo(methodOn(ComicBookController.class).toggleIsComicBookFavorite(comicId))
              .withRel("favorite"));
      List<Image> images = new ArrayList<>(entity.getImages());
      images.sort(Comparator.comparing(Image::getUri));
      if (!images.isEmpty()) {
        Image cover = images.get(0);
        model.add(
            linkTo(methodOn(ComicBookController.class).getPageData(cover.getId())).withRel("data"));
        for (int i = 0; i < images.size(); i++) {
          model.add(
              linkTo(methodOn(ComicBookController.class).getPageData(images.get(i).getId()))
                  .withRel("page_" + i));
        }
      }
      return model;
    }
  }
}
