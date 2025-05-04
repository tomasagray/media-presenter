package net.tomasbot.mp.api.service;

import jakarta.persistence.EntityManager;
import net.tomasbot.mp.api.service.user.UserComicService;
import net.tomasbot.mp.api.service.user.UserPictureService;
import net.tomasbot.mp.api.service.user.UserVideoService;
import net.tomasbot.mp.model.*;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserImageView;
import net.tomasbot.mp.user.UserVideoView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SearchService {

  private static final Logger logger = LogManager.getLogger(SearchService.class);
  private static final String[] SEARCH_FIELDS = {"title", "tags.name"};

  private final EntityManager entityManager;
  private final UserVideoService videoService;
  private final UserPictureService pictureService;
  private final UserComicService comicService;

  public SearchService(
      EntityManager entityManager,
      UserVideoService videoService,
      UserPictureService pictureService,
      UserComicService comicService) {
    this.entityManager = entityManager;
    this.videoService = videoService;
    this.pictureService = pictureService;
    this.comicService = comicService;
  }

  public SearchAllResult searchAllFor(@NotNull String query, @NotNull PageRequest request) {
    logger.info("Searching all Entities for: {}", query);
    return doIndividualSearch(query, request);

/*
    final List<Class<?>> types = List.of(Video.class, Picture.class, ComicBook.class);
    int offset = (int) request.getOffset();
    int pageSize = request.getPageSize() * types.size();
    Sort sort = request.getSort();

    // adapt the request to hold a page of each type
    PageRequest allRequest = PageRequest.of(offset, pageSize, sort);
    return doSearch(query, types, allRequest);

 */
  }

  public SearchAllResult searchVideosFor(@NotNull String query, PageRequest request) {
    logger.info("Searching Videos for: {}", query);
    List<Class<?>> types = List.of(Video.class);
    return doSearch(query, types, request);
  }

  public SearchAllResult searchPicturesFor(@NotNull String query, PageRequest request) {
    logger.info("Searching Pictures for: {}", query);
    List<Class<?>> types = List.of(Picture.class);
    return doSearch(query, types, request);
  }

  public SearchAllResult searchComicsFor(@NotNull String query, PageRequest request) {
    logger.info("Searching Comic Books for: {}", query);
    List<Class<?>> types = List.of(ComicBook.class);
    return doSearch(query, types, request);
  }

  /**
   * Ensure a clean search
   *
   * @param rawQuery The unmodified search query submitted by the user
   * @return a cleaned-up search query
   */
  @Contract(pure = true)
  private @NotNull String getNormalizedQuery(@NotNull String rawQuery) {
    return rawQuery.trim();
  }

  private SearchAllResult doIndividualSearch(String query, PageRequest request) {
    final String normalizedQuery = getNormalizedQuery(query);
    SearchResults<UserImageView> pictures = searchPicturesFor(normalizedQuery, request).getPictures();
    SearchResults<UserComicBookView> comics = searchComicsFor(normalizedQuery, request).getComics();
    SearchResults<UserVideoView> videos = searchVideosFor(normalizedQuery, request).getVideos();

    int totalCount = pictures.getSize() + comics.getSize() + videos.getSize();
    int limit = request.getPageSize();
    int nextOffset = (int) (request.getOffset() + limit);
    if (nextOffset < totalCount) nextOffset = 0;

    return SearchAllResult.builder()
            .totalResults(totalCount)
            .videos(videos)
            .pictures(pictures)
            .comics(comics)
            .offset(nextOffset)
            .limit(limit)
            .build();
  }

  private SearchAllResult doSearch(
      @NotNull String query, List<Class<?>> types, @NotNull PageRequest request) {
    int offset = (int) request.getOffset();
    int limit = request.getPageSize();
    logger.trace("Searching {} for '{}'; offset: {}, limit: {}", types, query, offset, limit);

    String normalizedQuery = getNormalizedQuery(query);
    SearchSession session = Search.session(entityManager);
    SearchResult<Object> results =
        session
            .search(types)
                .where(f -> f.match().fields(SEARCH_FIELDS).matching(normalizedQuery).fuzzy(2))
            .fetch(offset, limit);

    logger.info("Search '{}' found {} results", query, results.hits().size());
    return createResult(results, request);
  }

  private SearchAllResult createResult(
      @NotNull SearchResult<?> results, @NotNull PageRequest request) {
    List<UserVideoView> videos = new ArrayList<>();
    List<UserImageView> pictures = new ArrayList<>();
    List<UserComicBookView> comics = new ArrayList<>();

    for (Object result : results.hits()) {
      logger.trace("Sorting result: {}", result);

      if (result instanceof Video video) {
        videos.add(videoService.getUserVideoView(video));
      } else if (result instanceof Picture picture) {
        pictures.add(pictureService.getUserImageView(picture));
      } else if (result instanceof ComicBook comic) {
        comics.add(comicService.getUserComicBookView(comic));
      }
    }

    long hitCount = results.total().hitCount();
    int limit = request.getPageSize();
    int nextOffset = (int) (request.getOffset() + limit);
    if (nextOffset < hitCount) nextOffset = 0;

    logger.info(
        "Found: {} Videos, {} Pictures, {} Comic Books ({} total)",
        videos.size(),
        pictures.size(),
        comics.size(),
        hitCount);
    return SearchAllResult.builder()
        .videos(new SearchResults<>(videos, request, hitCount))
        .pictures(new SearchResults<>(pictures, request, hitCount))
        .comics(new SearchResults<>(comics, request, hitCount))
        .totalResults(hitCount)
        .offset(nextOffset)
        .limit(limit)
        .build();
  }
}
