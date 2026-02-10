package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.RandomComicCollectionRepo;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.RandomComicBookCollection;
import net.tomasbot.mp.model.RandomEntityCollection;
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
public class RandomComicService implements RandomEntityService<ComicBook> {

  private final RandomComicCollectionRepo repository;
  private final ComicBookService comicBookService;

  @Value("${application.config.rand-collection-max}")
  private int randomCollectionMax;

  public RandomComicService(RandomComicCollectionRepo repository, ComicBookService comicBookService) {
    this.repository = repository;
    this.comicBookService = comicBookService;
  }

  private @Nullable RandomComicBookCollection createRandomComics() {
    List<ComicBook> comics = comicBookService.getRandomComics(RandomEntityCollection.COLLECTION_SIZE);

    if (comics.size() < RandomEntityCollection.COLLECTION_SIZE) return null;

    RandomComicBookCollection collection = new RandomComicBookCollection(comics);
    return repository.save(collection);
  }

  @Override
  @Transactional
  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
  public RandomComicBookCollection addRandomCollection() {
    List<RandomComicBookCollection> all = repository.findAll();
    all.sort(Comparator.comparing(RandomEntityCollection::getCreated));

    if (all.size() >= randomCollectionMax) {
      // remove oldest
      RandomComicBookCollection remove = all.remove(0);
      repository.delete(remove);
    }

    // add new
    return createRandomComics();
  }

  @Override
  public List<ComicBook> getRandomCollection() {
    return repository.findRandom(PageRequest.ofSize(1))
            .stream()
            .map(RandomComicBookCollection::getComics)
            .flatMap(Set::stream)
            .toList();
  }
}
