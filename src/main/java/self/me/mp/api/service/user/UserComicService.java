package self.me.mp.api.service.user;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import self.me.mp.api.service.ComicBookService;
import self.me.mp.model.ComicBook;
import self.me.mp.model.UserPreferences;
import self.me.mp.user.UserComicBookView;
import self.me.mp.user.UserComicBookView.UserComicModeller;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserComicService {

	private final UserService userService;
	private final ComicBookService comicService;
	private final UserComicModeller modeller;

	public UserComicService(
			UserService userService,
			ComicBookService comicService,
			UserComicModeller modeller) {
		this.userService = userService;
		this.comicService = comicService;
		this.modeller = modeller;
	}

	public Page<UserComicBookView> getAllUserComics(int page, int size) {
		return comicService.getAllComics(page, size).map(this::getUserComicBookView);
	}

	public Page<UserComicBookView> getLatestUserComics(int page, int size) {
		return comicService.getLatestComics(page, size).map(this::getUserComicBookView);
	}

	@NotNull
	private UserComicBookView getUserComicBookView(@NotNull ComicBook comic) {
		return userService.getUserPreferences().isFavorite(comic) ?
				modeller.toFavorite(comic) : modeller.toView(comic);
	}

	public Collection<UserComicBookView> getUserComicViews(@NotNull Collection<ComicBook> comics) {
		return comics.stream().map(this::getUserComicBookView).toList();
	}

	public List<UserComicBookView> getRandomUserComics(int count) {
		return comicService.getRandomComics(count).stream().map(this::getUserComicBookView).toList();
	}

	public Optional<UserComicBookView> getUserComicBook(UUID comicId) {
		return comicService.getComicBook(comicId).map(this::getUserComicBookView);
	}

	public UserComicBookView toggleIsComicFavorite(@NotNull UUID comicId) {
		Optional<ComicBook> optional = comicService.getComicBook(comicId);
		if (optional.isEmpty()) {
			throw new IllegalArgumentException("Trying to favorite non-existent Comic Book: " + comicId);
		}
		ComicBook comic = optional.get();
		UserPreferences preferences = userService.getUserPreferences();
		if (preferences.toggleFavorite(comic)) {
			return modeller.toFavorite(comic);
		}
		return modeller.toView(comic);
	}

	public Collection<UserComicBookView> getFavoriteComics() {
		return userService.getUserPreferences()
				.getFavoriteComics().stream()
				.map(this::getUserComicBookView)
				.toList();
	}

	public Optional<UrlResource> getPageData(UUID pageId) {
		return comicService.getPageData(pageId);
	}

	public MultiValueMap<String, Path> getInvalidFiles() {
		return comicService.getInvalidFiles();
	}
}
