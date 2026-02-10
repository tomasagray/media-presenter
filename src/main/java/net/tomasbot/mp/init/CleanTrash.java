package net.tomasbot.mp.init;

import net.tomasbot.mp.util.TrashCollectorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Order
public class CleanTrash implements CommandLineRunner {

  private final TrashCollectorService trashCollectorService;

  public CleanTrash(TrashCollectorService trashCollectorService) {
    this.trashCollectorService = trashCollectorService;
  }

  @Override
  public void run(String... args) throws Exception {
    scheduledCleanup();
  }

  @Scheduled(cron = "${application.config.trash-cleanup}")
  public void scheduledCleanup() throws Exception {
    trashCollectorService.cleanVideoTrash();
    trashCollectorService.deleteStrayThumbnails();

    trashCollectorService.cleanPictureTrash();
    trashCollectorService.cleanComicsTrash();
  }
}
