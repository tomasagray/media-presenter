package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.RandomVideoCollectionRepo;
import net.tomasbot.mp.model.RandomEntityCollection;
import net.tomasbot.mp.model.RandomVideoCollection;
import net.tomasbot.mp.model.Video;
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
public class RandomVideoService implements RandomEntityService<Video> {

  private static final Logger logger = LogManager.getLogger(RandomVideoService.class);

  private final RandomVideoCollectionRepo repository;
  private final VideoService videoService;

  @Value("${application.config.rand-collection-max}")
  private int randomCollectionMax;

  public RandomVideoService(RandomVideoCollectionRepo repository, VideoService videoService) {
    this.repository = repository;
    this.videoService = videoService;
  }

  @Override
  @Transactional
  public RandomVideoCollection addRandomCollection() {
    logger.info("Creating random Video collections...");
    List<Video> videos = videoService.getRandom(RandomEntityCollection.COLLECTION_SIZE);

    if (videos.size() < RandomEntityCollection.COLLECTION_SIZE) return null;

    RandomVideoCollection collection = repository.saveAndFlush(new RandomVideoCollection(videos));
    logger.info("Created random collection with {} videos.", collection.size());
    return collection;
  }

  @Override
  @Transactional
  public void limitCollection() {
    this.limitCollections(repository, randomCollectionMax);
  }

  @Override
  public List<Video> getRandomCollection() {
    return repository.findRandom(PageRequest.ofSize(1))
            .stream()
            .map(RandomVideoCollection::getVideos)
            .flatMap(Set::stream)
            .toList();
  }

  @Override
  @Transactional
  public void deleteContaining(@NotNull UUID entityId) {
    repository.findAll()
            .stream()
            .filter(collection ->
                    collection.getVideos().stream().anyMatch(video -> video.getId().equals(entityId)))
            .map(RandomEntityCollection::getId)
            .forEach(repository::deleteById);
  }
}
