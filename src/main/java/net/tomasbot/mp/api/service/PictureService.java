package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.PictureRepository;
import net.tomasbot.mp.model.Picture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PictureService {

  private static final Logger logger = LogManager.getLogger(PictureService.class);
  private static final int RANDOM_PAGE_SIZE = 100;

  private final PictureRepository pictureRepo;
  private final TagManagementService tagService;
  private final Set<Picture> randomPictures = new HashSet<>();

  public PictureService(PictureRepository pictureRepo, TagManagementService tagService) {
    this.pictureRepo = pictureRepo;
    this.tagService = tagService;
  }

  public void save(@NotNull Picture picture) {
    pictureRepo.saveAndFlush(picture);
  }

  public void saveAll(@NotNull Iterable<? extends Picture> pictures) {
    pictureRepo.saveAll(pictures);
  }

  public Page<Picture> getAll(int page, int pageSize) {
    return pictureRepo.findAll(PageRequest.of(page, pageSize));
  }

  public Page<Picture> getLatestPictures(int page, int pageSize) {
    return pictureRepo.findLatest(PageRequest.of(page, pageSize));
  }

  @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
  public void setRandomPictures() {
    final PageRequest request = PageRequest.of(0, RANDOM_PAGE_SIZE);
    List<Picture> random = pictureRepo.findRandom(request);

    randomPictures.clear();
    randomPictures.addAll(random);
  }

  public List<Picture> getRandomPictures(int count) {
    if (randomPictures.isEmpty()) {
      return pictureRepo.findRandom(PageRequest.ofSize(count));
    }

    return RandomEntitySelector.selectRandom(randomPictures, count);
  }

  public List<Picture> getUnprocessedPictures() {
    return pictureRepo.findUnprocessedPictures();
  }

  public Optional<Picture> getPicture(@NotNull UUID picId) {
    return pictureRepo.findById(picId);
  }

  public Optional<UrlResource> getPictureData(@NotNull UUID picId) {
    return getPicture(picId).map(img -> UrlResource.from(img.getUri()));
  }

  public List<Picture> getPictureByPath(@NotNull Path path) {
    return pictureRepo.findAllByUri(path.toUri());
  }

  public Picture updatePicture(@NotNull Picture update) {
    logger.info("Updating Picture: {}", update);

    final UUID pictureId = update.getId();
    return (Picture)
        pictureRepo
            .findById(pictureId)
            .map(existing -> tagService.update(existing, update))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Cannot update non-existent Picture: " + pictureId));
  }

  public void deletePicture(@NotNull UUID picId) {
    logger.info("Deleting Picture: {}", picId);
    pictureRepo.deleteById(picId);
  }
}
