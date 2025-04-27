package net.tomasbot.mp.user;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserComicBookView extends UserView {

  private UUID id;
  private String title;
  private Timestamp timestamp;
  private Collection<Image> images;
  private Collection<Tag> tags;

  @Component
  public static class UserComicModeller extends UserViewModeller<ComicBook, UserComicBookView> {

    @Override
    public UserComicBookView toView(@NotNull ComicBook data) {
      UserComicBookView view = new UserComicBookView();
      view.setId(data.getId());
      view.setTitle(data.getTitle());
      view.setTimestamp(data.getAdded());
      view.setImages(data.getImages());
      view.setTags(data.getTags());
      return view;
    }

    @Override
    public ComicBook fromView(@NotNull UserComicBookView data) {
      Collection<Tag> tags = data.getTags();
      Collection<Image> pages = data.getImages();

      ComicBook comicBook = new ComicBook();
      comicBook.setId(data.getId());
      comicBook.setTitle(data.getTitle());
      if (tags != null) comicBook.getTags().addAll(tags);
      if (pages != null) comicBook.getImages().addAll(pages);
      return comicBook;
    }
  }
}
