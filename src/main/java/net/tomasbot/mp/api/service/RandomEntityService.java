package net.tomasbot.mp.api.service;

import net.tomasbot.mp.model.RandomEntityCollection;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface RandomEntityService<T> {

  RandomEntityCollection<T> addRandomCollection();

  @Transactional
  default Collection<RandomEntityCollection<?>> addRandomCollections(int count) {
    List<RandomEntityCollection<?>> collections = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      RandomEntityCollection<?> collection = addRandomCollection();
      if (collection != null) collections.add(collection);
    }

    return collections;
  }

  List<T> getRandomCollection();
}
