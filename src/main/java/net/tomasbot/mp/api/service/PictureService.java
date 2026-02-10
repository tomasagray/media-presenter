package net.tomasbot.mp.api.service;

import net.tomasbot.mp.db.PictureRepository;
import net.tomasbot.mp.model.Picture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PictureService {

  private static final Logger logger = LogManager.getLogger(PictureService.class);

  private final PictureRepository pictureRepo;
  private final TagManagementService tagService;

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

  public List<Picture> getAll() {
    return pictureRepo.findAll();
  }

  public Page<Picture> getAll(int page, int pageSize) {
    return pictureRepo.findAll(PageRequest.of(page, pageSize));
  }

  public Page<Picture> getLatestPictures(int page, int pageSize) {
    return pictureRepo.findLatest(PageRequest.of(page, pageSize));
  }

  public List<Picture> getRandomPictures(int count) {
    return pictureRepo.findRandom(PageRequest.ofSize(count));
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
