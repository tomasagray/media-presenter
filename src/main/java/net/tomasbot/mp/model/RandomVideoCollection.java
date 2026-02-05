package net.tomasbot.mp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
public class RandomVideoCollection extends RandomEntityCollection<Video> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToMany
  private final Set<Video> videos = new HashSet<>(COLLECTION_SIZE);

  public RandomVideoCollection(Collection<Video> videos) {
    this.videos.addAll(videos);
  }

  @Override
  public void add(@NonNull Video entity) {
    super.add(entity, this.videos);
  }

  @Override
  public void addAll(@NotNull Collection<Video> entities) {
    super.addAll(entities, this.videos);
  }

  @Override
  public void remove(Video entity) {
    super.remove(entity, this.videos);
  }
}
