package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.RandomComicCollectionRepo;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.RandomComicBookCollection;
import net.tomasbot.mp.model.RandomEntityCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RandomComicService implements RandomEntityService<ComicBook> {

  private static final Logger logger = LogManager.getLogger(RandomComicService.class);

  private final RandomComicCollectionRepo repository;
  private final ComicBookService comicBookService;

  @Value("${application.config.rand-collection-max}")
  private int randomCollectionMax;

  public RandomComicService(RandomComicCollectionRepo repository, ComicBookService comicBookService) {
    this.repository = repository;
    this.comicBookService = comicBookService;
  }

  @Override
  @Transactional
  public RandomComicBookCollection addRandomCollection() {
    logger.info("Creating random Comic Book collections...");
    List<ComicBook> comics = comicBookService.getRandomComics(RandomEntityCollection.COLLECTION_SIZE);

    if (comics.size() < RandomEntityCollection.COLLECTION_SIZE) return null;

    RandomComicBookCollection collection = repository.saveAndFlush(new RandomComicBookCollection(comics));
    logger.info("Created random Comic Book collection with {} comics.", collection.size());
    return collection;
  }

  @Override
  @Transactional
  public void limitCollection() {
    this.limitCollections(repository, randomCollectionMax);
  }

  @Override
  public List<ComicBook> getRandomCollection() {
    return repository.findRandom(PageRequest.ofSize(1))
            .stream()
            .map(RandomComicBookCollection::getComics)
            .flatMap(Set::stream)
            .toList();
  }

  @Override
  public void deleteContaining(@NotNull UUID entityId) {
    repository.findAll()
            .stream()
            .filter(collection ->
                    collection.getComics().stream().anyMatch(comic -> comic.getId().equals(entityId)))
            .map(RandomEntityCollection::getId)
            .forEach(repository::deleteById);
  }
}
