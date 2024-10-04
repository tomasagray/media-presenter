package net.tomasbot.mp.model;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserData {

  public record Favorite(UUID id, String title, Path path) {}

  private String username;

  private final Set<Favorite> favoriteVideos = new HashSet<>();
  private final Set<Favorite> favoritePictures = new HashSet<>();
  private final Set<Favorite> favoriteComics = new HashSet<>();

  public UserData(String username) {
    this.username = username;
  }
}
