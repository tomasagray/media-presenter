package net.tomasbot.mp.api.service.user;

public class SetFavoriteException extends IllegalStateException {
  public SetFavoriteException(Object o) {
    super("Could not set favorite on: " + o);
  }
}
