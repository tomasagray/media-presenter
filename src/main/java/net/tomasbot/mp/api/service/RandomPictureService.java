package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.RandomPictureCollectionRepo;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.RandomEntityCollection;
import net.tomasbot.mp.model.RandomPictureCollection;
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

import static net.tomasbot.mp.model.RandomEntityCollection.COLLECTION_SIZE;

@Service
public class RandomPictureService implements RandomEntityService<Picture> {

  private final RandomPictureCollectionRepo repository;
  private final PictureService pictureService;

  @Value("${application.config.rand-collection-max}")
  private int randomCollectionMax;

  public RandomPictureService(RandomPictureCollectionRepo repository, PictureService pictureService) {
    this.repository = repository;
    this.pictureService = pictureService;
  }

  private @Nullable RandomPictureCollection createRandomPictures() {
    List<Picture> pictures = pictureService.getRandomPictures(COLLECTION_SIZE);

    if (pictures.size() < COLLECTION_SIZE) return null;

    RandomPictureCollection collection = new RandomPictureCollection(pictures);
    return repository.saveAndFlush(collection);
  }

  @Override
  @Transactional
  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
  public RandomPictureCollection addRandomCollection() {
    List<RandomPictureCollection> all = repository.findAll();
    all.sort(Comparator.comparing(RandomEntityCollection::getCreated));

    if (all.size() >= randomCollectionMax) {
      // remove oldest
      RandomPictureCollection remove = all.remove(0);
      repository.delete(remove);
    }

    // add new
    return createRandomPictures();
  }

  @Override
  public List<Picture> getRandomCollection() {
    List<RandomPictureCollection> randomCollections = repository.findRandom(PageRequest.ofSize(1));
    return randomCollections.stream()
            .map(RandomPictureCollection::getPictures)
            .flatMap(Set::stream)
            .toList();
  }
}
