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
public class RandomPictureCollection extends RandomEntityCollection<Picture> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToMany
  private final Set<Picture> pictures = new HashSet<>(COLLECTION_SIZE);

  public RandomPictureCollection(Collection<Picture> pictures) {
    this.pictures.addAll(pictures);
  }

  @Override
  public void add(@NonNull Picture entity) {
    super.add(entity, this.pictures);
  }

  @Override
  public void addAll(@NotNull Collection<Picture> entities) {
    super.addAll(entities, this.pictures);
  }

  @Override
  public void remove(Picture entity) {
    super.remove(entity, this.pictures);
  }
}
