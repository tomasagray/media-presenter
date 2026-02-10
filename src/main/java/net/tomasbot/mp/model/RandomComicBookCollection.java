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
public class RandomComicBookCollection extends RandomEntityCollection<ComicBook> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToMany
  private final Set<ComicBook> comics = new HashSet<>(COLLECTION_SIZE);

  public RandomComicBookCollection(Collection<ComicBook> comics) {
    this.comics.addAll(comics);
  }

  @Override
  public void add(@NonNull ComicBook entity) {
    super.add(entity, this.comics);
  }

  @Override
  public void addAll(@NotNull Collection<ComicBook> entities) {
    super.addAll(entities, this.comics);
  }

  @Override
  public void remove(ComicBook entity) {
    super.remove(entity, this.comics);
  }
}
