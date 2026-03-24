package net.tomasbot.mp.api.service.search;

import jakarta.persistence.EntityManager;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.Image;
import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.Video;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class SearchIndexService {

  private static final Logger logger = LogManager.getLogger(SearchIndexService.class);
  public static final List<Class<?>> TYPES =
          List.of(Video.class, Picture.class, Image.class, ComicBook.class);

  private final EntityManager entityManager;

  public SearchIndexService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  private static @NonNull @Unmodifiable Collection<String> getTypeNames(@NotNull Collection<Class<?>> types) {
    return types.stream()
            .filter(Objects::nonNull)
            .map(Class::getSimpleName)
            .toList();
  }

  @Transactional
  public void index(@NotNull Collection<Class<?>> types) {
    try {
      SearchSession session = Search.session(entityManager);

      Instant start = Instant.now();
      session.massIndexer(types).startAndWait();
      Instant end = Instant.now();

      long duration = Duration.between(start, end).toMillis();
      logger.info("Search indexing {} took: {}ms", getTypeNames(types), duration);
    } catch (InterruptedException e) {
      logger.error("Search indexing interrupted", e);
    }
  }

  @Transactional
  public void index(Class<?> type) {
    this.index(List.of(type));
  }

  @Transactional
  public void indexAll() {
    this.index(TYPES);
  }
}
