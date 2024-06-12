package net.tomasbot.mp.user;

import java.util.Collection;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserImageView extends UserView {

  private UUID id;
  private String title;
  private int width;
  private int height;
  private long filesize;
  private Collection<Tag> tags;

  @Component
  public static class UserImageModeller extends UserViewModeller<Image, UserImageView> {

    @Override
    public UserImageView toView(@NotNull Image data) {
      UserImageView view = new UserImageView();
      view.setId(data.getId());
      view.setTitle(data.getTitle());
      view.setWidth(data.getWidth());
      view.setHeight(data.getHeight());
      view.setFilesize(data.getFilesize());
      view.setTags(data.getTags());
      return view;
    }
  }
}
