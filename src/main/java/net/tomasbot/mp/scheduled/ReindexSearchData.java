package net.tomasbot.mp.scheduled;

import net.tomasbot.mp.api.service.search.SearchIndexService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReindexSearchData {

  private static final Logger logger = LogManager.getLogger(ReindexSearchData.class);

  private final SearchIndexService searchIndexService;

  public ReindexSearchData(SearchIndexService searchIndexService) {
    this.searchIndexService = searchIndexService;
  }

  @Scheduled(cron = "${application.config.search.reindex-schedule}")
  public void reindex() {
    logger.info("Performing scheduled reindex of search data...");
    searchIndexService.indexAll();
    logger.info("Scheduled reindex of search data finished.");
  }
}
