package net.tomasbot.mp.scheduled;

import net.tomasbot.mp.util.TrashCollectorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanTrash {

  private final TrashCollectorService trashCollectorService;

  public CleanTrash(TrashCollectorService trashCollectorService) {
    this.trashCollectorService = trashCollectorService;
  }

  @Scheduled(cron = "${application.config.trash-cleanup-schedule}")
  public void scheduledCleanup() throws Exception {
    trashCollectorService.cleanVideoTrash();
    trashCollectorService.deleteStrayThumbnails();
    trashCollectorService.deleteBrokenThumbnails();

    trashCollectorService.cleanPictureTrash();
    trashCollectorService.cleanComicsTrash();
  }
}
