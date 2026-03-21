package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.RandomPictureCollectionRepo;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.RandomEntityCollection;
import net.tomasbot.mp.model.RandomPictureCollection;
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

import static net.tomasbot.mp.model.RandomEntityCollection.COLLECTION_SIZE;

@Service
public class RandomPictureService implements RandomEntityService<Picture> {

  private static final Logger logger = LogManager.getLogger(RandomPictureService.class);

  private final RandomPictureCollectionRepo repository;
  private final PictureService pictureService;

  @Value("${application.config.rand-collection-max}")
  private int randomCollectionMax;

  public RandomPictureService(RandomPictureCollectionRepo repository, PictureService pictureService) {
    this.repository = repository;
    this.pictureService = pictureService;
  }

  @Override
  @Transactional
  public RandomPictureCollection addRandomCollection() {
    logger.info("Creating random Picture collections...");
    List<Picture> pictures = pictureService.getRandomPictures(COLLECTION_SIZE);

    if (pictures.size() < COLLECTION_SIZE) return null;

    RandomPictureCollection collection = repository.saveAndFlush(new RandomPictureCollection(pictures));
    logger.info("Created random Picture collection with {} pictures.", collection.size());
    return collection;
  }

  @Override
  @Transactional
  public void limitCollection() {
    this.limitCollections(repository, randomCollectionMax);
  }

  @Override
  public List<Picture> getRandomCollection() {
    List<RandomPictureCollection> randomCollections = repository.findRandom(PageRequest.ofSize(1));
    return randomCollections.stream()
            .map(RandomPictureCollection::getPictures)
            .flatMap(Set::stream)
            .toList();
  }

  @Override
  public void deleteContaining(@NotNull UUID entityId) {
    repository.findAll()
            .stream()
            .filter(collection ->
                    collection.getPictures().stream().anyMatch(pic -> pic.getId().equals(entityId)))
            .map(RandomEntityCollection::getId)
            .forEach(repository::deleteById);
  }
}
