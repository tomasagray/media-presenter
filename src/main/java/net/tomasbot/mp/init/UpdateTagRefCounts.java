package net.tomasbot.mp.init;

import net.tomasbot.mp.api.service.TagCreationService;
import org.apache.logging.log4j.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order // Default: Integer.MAX_VALUE; run last
public class UpdateTagRefCounts implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(UpdateTagRefCounts.class);

  private final TagCreationService tagService;

  public UpdateTagRefCounts(TagCreationService tagService) {
    this.tagService = tagService;
  }

  @Override
  public void run(String... args) throws Exception {
    logger.info("Beginning initial tag reference count update...");
    tagService.updateTagRefCounts();
  }
}
