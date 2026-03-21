package net.tomasbot.mp.init;

import net.tomasbot.mp.api.service.TagCreationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order // Default: Integer.MAX_VALUE; run last
public class UpdateTagRefCounts implements CommandLineRunner {

  private final TagCreationService tagService;

  public UpdateTagRefCounts(TagCreationService tagService) {
    this.tagService = tagService;
  }

  @Override
  public void run(String... args) throws Exception {
    tagService.updateTagRefCounts();
  }
}
