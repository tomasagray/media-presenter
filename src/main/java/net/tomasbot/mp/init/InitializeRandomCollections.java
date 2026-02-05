package net.tomasbot.mp.init;

import net.tomasbot.mp.api.service.RandomComicService;
import net.tomasbot.mp.api.service.RandomPictureService;
import net.tomasbot.mp.api.service.RandomVideoService;
import net.tomasbot.mp.model.RandomEntityCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Component
@Order
public class InitializeRandomCollections implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(InitializeRandomCollections.class);
  private static final int STARTUP_CREATE_COUNT = 20;

  private final RandomVideoService videoService;
  private final RandomPictureService pictureService;
  private final RandomComicService comicService;

  public InitializeRandomCollections(
          RandomVideoService videoService, RandomPictureService pictureService, RandomComicService comicService) {
    this.videoService = videoService;
    this.pictureService = pictureService;
    this.comicService = comicService;
  }

  @Override
//  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void run(String... args) throws Exception {
    logger.info("Creating random Video collections...");
    Collection<RandomEntityCollection<?>> randomVideos = videoService.addRandomCollections(STARTUP_CREATE_COUNT);
    logger.info("Created {} random Video collections.", randomVideos.size());

    logger.info("Creating random Picture collections...");
    Collection<RandomEntityCollection<?>> randomPix = pictureService.addRandomCollections(STARTUP_CREATE_COUNT);
    logger.info("Created {} random Picture collections.", randomPix.size());

    logger.info("Creating random Comic Book collections...");
    Collection<RandomEntityCollection<?>> randomComics = comicService.addRandomCollections(STARTUP_CREATE_COUNT);
    logger.info("Created {} random Comic Book collections.", randomComics.size());
  }
}
