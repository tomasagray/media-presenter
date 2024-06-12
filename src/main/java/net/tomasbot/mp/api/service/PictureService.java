package net.tomasbot.mp.api.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.db.PictureRepository;
import net.tomasbot.mp.model.Picture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class PictureService {

  private static final Logger logger = LogManager.getLogger(PictureService.class);
  private final PictureRepository pictureRepo;

  public PictureService(PictureRepository pictureRepo) {
    this.pictureRepo = pictureRepo;
  }

  public void save(@NotNull Picture picture) {
    pictureRepo.save(picture);
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
    return pictureRepo.findAll().stream()
        .filter(pic -> pic.getUri().getPath().startsWith(path.toString()))
        .toList();
  }

  public void deletePicture(@NotNull UUID picId) {
    logger.info("Deleting Picture: {}", picId);
    pictureRepo.deleteById(picId);
  }
}
