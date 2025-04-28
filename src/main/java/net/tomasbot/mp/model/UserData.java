package net.tomasbot.mp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UserData {

  @Data
  @AllArgsConstructor
  public static class Favorite {
    private UUID id;
    private String title;
    private Path path;
  }

  private String username;

  private final Set<Favorite> favoriteVideos = new HashSet<>();
  private final Set<Favorite> favoritePictures = new HashSet<>();
  private final Set<Favorite> favoriteComics = new HashSet<>();

  public UserData(String username) {
    this.username = username;
  }
}
