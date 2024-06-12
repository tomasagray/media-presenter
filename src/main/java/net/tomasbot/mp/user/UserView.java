package net.tomasbot.mp.user;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public abstract class UserView {

  private boolean isFavorite;

  public abstract static class UserViewModeller<U, S extends UserView> {

    public abstract S toView(@NotNull U data);

    public S toFavorite(@NotNull U data) {
      final S view = toView(data);
      view.setFavorite(true);
      return view;
    }
  }
}
