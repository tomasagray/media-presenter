package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.RandomVideoCollectionRepo;
import net.tomasbot.mp.model.RandomEntityCollection;
import net.tomasbot.mp.model.RandomVideoCollection;
import net.tomasbot.mp.model.Video;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RandomVideoService implements RandomEntityService<Video> {

  private final RandomVideoCollectionRepo repository;
  private final VideoService videoService;

  @Value("${application.config.rand-collection-max}")
  private int randomCollectionMax;

  public RandomVideoService(RandomVideoCollectionRepo repository, VideoService videoService) {
    this.repository = repository;
    this.videoService = videoService;
  }

  private @Nullable RandomVideoCollection createRandomVideos() {
    List<Video> videos = videoService.getRandom(RandomEntityCollection.COLLECTION_SIZE);

    if (videos.size() < RandomEntityCollection.COLLECTION_SIZE) return null;

    RandomVideoCollection collection = new RandomVideoCollection(videos);
    return repository.saveAndFlush(collection);
  }

  @Override
  @Transactional
  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
  public RandomVideoCollection addRandomCollection() {
    List<RandomVideoCollection> all = repository.findAll();
    all.sort(Comparator.comparing(RandomEntityCollection::getCreated));

    if (all.size() >= randomCollectionMax) {
      // remove oldest
      RandomVideoCollection remove = all.remove(0);
      repository.delete(remove);
    }

    // add new
    return createRandomVideos();
  }

  @Override
  public List<Video> getRandomCollection() {
    return repository.findRandom(PageRequest.ofSize(1))
            .stream()
            .map(RandomVideoCollection::getVideos)
            .flatMap(Set::stream)
            .toList();
  }
}
