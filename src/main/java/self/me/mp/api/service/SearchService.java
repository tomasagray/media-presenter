package self.me.mp.api.service;

import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Picture;
import self.me.mp.model.SearchAllResult;
import self.me.mp.model.Video;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

	private static final Logger logger = LogManager.getLogger(SearchService.class);

	private final EntityManager entityManager;
	private final VideoService videoService;
	private final PictureService pictureService;
	private final ComicBookService comicService;

	public SearchService(
			EntityManager entityManager,
			VideoService videoService,
			PictureService pictureService,
			ComicBookService comicService) {
		this.entityManager = entityManager;
		this.videoService = videoService;
		this.pictureService = pictureService;
		this.comicService = comicService;
	}

	public SearchAllResult searchFor(@NotNull String query, int offset, int limit) {
		logger.info("Searching all Entities for: {}", query);
		logger.trace("Search offset: {}; limit: {}", offset, limit);

		SearchSession session = Search.session(entityManager);
		List<Class<?>> types = List.of(Video.class, Picture.class, ComicBook.class);
		SearchResult<Object> results =
				session.search(types)
						.where(f ->
								f.match().fields("title", "tags.name")
										.matching(query))
						.fetch(offset, limit);
		return createResult(results, offset, limit);
	}

	private SearchAllResult createResult(@NotNull SearchResult<?> results, int offset, int limit) {

		List<Video> videos = new ArrayList<>();
		List<Picture> pictures = new ArrayList<>();
		List<ComicBook> comics = new ArrayList<>();
		long hitCount = results.total().hitCount();
		int nextOffset = offset + limit;
		if (nextOffset < hitCount) {
			nextOffset = 0;
		}

		for (Object result : results.hits()) {
			if (result instanceof Video video) {
				videos.add(video);
			} else if (result instanceof Picture picture) {
				pictures.add(picture);
			} else if (result instanceof ComicBook comic) {
				comics.add(comic);
			}
		}

		logger.info("Found: {} Videos, {} Pictures, {} Comic Books ({} total)",
				videos.size(), pictures.size(), comics.size(), hitCount);
		return SearchAllResult.builder()
				.videos(videoService.getUserVideoViews(videos))
				.pictures(pictureService.getUserImageViews(pictures))
				.comics(comicService.getUserComicViews(comics))
				.totalResults(hitCount)
				.offset(nextOffset)
				.limit(limit)
				.build();
	}
}
