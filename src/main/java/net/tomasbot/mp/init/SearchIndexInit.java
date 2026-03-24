package net.tomasbot.mp.init;

import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static net.tomasbot.mp.api.service.search.SearchIndexService.TYPES;

@Component
@Transactional
public class SearchIndexInit implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(SearchIndexInit.class);

  private final EntityManager entityManager;

  public SearchIndexInit(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void run(String... args) throws Exception {
    SearchSession session = Search.session(entityManager);
    MassIndexer massIndexer = session.massIndexer(TYPES);

    // Ensure the schema exists without blowing away existing data
    session.schemaManager(TYPES).createIfMissing();

    // Check if any documents are already indexed
    long indexedCount = session.search(TYPES)
            .where(SearchPredicateFactory::matchAll)
            .fetchTotalHitCount();

    if (indexedCount == 0) {
      logger.info("Index is empty — running mass indexer...");
      Instant start = Instant.now();
      massIndexer.startAndWait();
      Instant end = Instant.now();

      long indexDuration = Duration.between(start, end).toMillis();
      logger.info("Done with initial search index. Indexing took: {}ms", indexDuration);
    }
  }
}
