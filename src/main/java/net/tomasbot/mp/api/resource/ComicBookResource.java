package net.tomasbot.mp.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.sql.Timestamp;
import java.util.*;
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

  private static final String PAGE_REL_PREFIX = "page_";

  private Timestamp timestamp;
  private int pageCount;

  @Component
  public static class ComicBookModeller
      extends RepresentationModelAssemblerSupport<UserComicBookView, ComicBookResource> {

    public ComicBookModeller() {
      super(ComicBookController.class, ComicBookResource.class);
    }

    private static void addPageLinks(
        @NotNull Collection<Image> images, @NotNull ComicBookResource model) {
      List<Image> pages = new ArrayList<>(images);
      int pageCount = pages.size();
      model.setPageCount(pageCount);
      pages.sort(Comparator.comparing(Image::getUri));

      if (pageCount > 0) {
        Image cover = pages.get(0);
        model.add(
            linkTo(methodOn(ComicBookController.class).getPageData(cover.getId()))
                .withRel(DATA_REL));
        for (int i = 0; i < pageCount; i++) {
          model.add(
              linkTo(methodOn(ComicBookController.class).getPageData(pages.get(i).getId()))
                  .withRel(PAGE_REL_PREFIX + i));
        }
      }
    }

    @Override
    public @NotNull ComicBookResource toModel(@NotNull UserComicBookView entity) {
      ComicBookResource model = instantiateModel(entity);

      UUID comicId = entity.getId();
      Collection<Image> images = entity.getImages();

      model.setId(comicId);
      model.setTitle(entity.getTitle());
      model.setTimestamp(entity.getTimestamp());
      model.setTags(entity.getTags());
      model.setFavorite(entity.isFavorite());

      // attach links
      if (images != null) addPageLinks(images, model);
      model.add(linkTo(methodOn(ComicBookController.class).getComic(comicId)).withSelfRel());
      model.add(
          linkTo(methodOn(ComicBookController.class).toggleIsComicBookFavorite(comicId))
              .withRel(FAVORITE_REL));
      model.add(
          linkTo(methodOn(ComicBookController.class).updateComicBook(model)).withRel(UPDATE_REL));

      return model;
    }

    public UserComicBookView fromModel(@NotNull ComicBookResource resource) {
      UserComicBookView comicView = new UserComicBookView();
      comicView.setId(resource.getId());
      comicView.setTitle(resource.getTitle());
      comicView.setTags(resource.getTags());
      comicView.setTimestamp(resource.getTimestamp());
      return comicView;
    }
  }
}
