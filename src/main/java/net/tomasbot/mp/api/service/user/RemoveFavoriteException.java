package net.tomasbot.mp.api.service.user;

public class RemoveFavoriteException extends IllegalStateException {
  public RemoveFavoriteException(Object o) {
    super("Could not remove favorite on: " + o);
  }
}
