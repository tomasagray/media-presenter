package net.tomasbot.mp.model;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;

@Getter
public abstract class RandomEntityCollection<T> {

  public static final int COLLECTION_SIZE = 18;

  private final Timestamp created = Timestamp.from(Instant.now());

  public abstract void add(@NotNull T entity);

  protected void add(@NotNull T entity, @NotNull Collection<T> entities) {
    if (entities.size() < COLLECTION_SIZE) entities.add(entity);
    else failFull();
  }

  public abstract void addAll(@NotNull Collection<T> entities);

  protected void addAll(@NonNull Collection<T> entities, @NotNull Collection<T> _entities) {
    if (entities.isEmpty()) throw new IllegalArgumentException("Cannot add empty collection");

    if (_entities.size() + entities.size() <= COLLECTION_SIZE)
      _entities.addAll(entities);
    else failFull();
  }

  public abstract void remove(T entity);

  protected void remove(T entity, @NonNull Collection<T> entities) {
    entities.remove(entity);
  }

  private void failFull() {
    throw new IllegalStateException(String.format("Cannot add to collection: at capacity (%d)", COLLECTION_SIZE));
  }
}
