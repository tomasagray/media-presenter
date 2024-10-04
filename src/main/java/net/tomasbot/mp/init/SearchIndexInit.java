package net.tomasbot.mp.init;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.Video;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SearchIndexInit implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(SearchIndexInit.class);
  private static final List<Class<?>> types =
      List.of(Video.class, Picture.class, Image.class, ComicBook.class);

  private final EntityManager entityManager;

  public SearchIndexInit(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void run(String... args) throws Exception {
    logger.info("Beginning initial search index of: {} ...", types);

    SearchSession session = Search.session(entityManager);
    MassIndexer massIndexer = session.massIndexer(types);

    final Instant start = Instant.now();
    massIndexer.startAndWait();
    final Instant end = Instant.now();

    final long indexDuration = Duration.between(start, end).toMillis();
    logger.info("Done with initial search index. Indexing took: {}ms", indexDuration);
  }
}
