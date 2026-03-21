package net.tomasbot.mp.api.service;

import net.tomasbot.mp.model.RandomEntityCollection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

public interface RandomEntityService<T> {

  @Transactional
  default Collection<RandomEntityCollection<?>> addRandomCollections(int count) {
    List<RandomEntityCollection<?>> collections = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      RandomEntityCollection<?> collection = addRandomCollection();
      if (collection != null) collections.add(collection);
    }

    return collections;
  }

  default <C extends RandomEntityCollection<?>> void limitCollections(
          @NonNull JpaRepository<C, Long> repository, int max) {
    List<C> all = repository.findAll();
    all.sort(Comparator.comparing(RandomEntityCollection::getCreated));

    if (all.size() >= max) {
      // remove oldest
      C remove = all.remove(0);
      repository.delete(remove);
    }
  }

  List<T> getRandomCollection();

  void limitCollection();

  RandomEntityCollection<T> addRandomCollection();

  void deleteContaining(@NotNull UUID entityId);
}
