package self.me.mp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
public class UserPreferences {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	private String username;

	@OneToMany
	private Set<Video> favoriteVideos;
	@OneToMany
	private Set<Picture> favoritePictures;
	@OneToMany
	private Set<ComicBook> favoriteComics;

	public UserPreferences(String username) {
		this.username = username;
	}

	public boolean isFavorite(Video video) {
		return favoriteVideos.contains(video);
	}

	public boolean isFavorite(Picture picture) {
		return favoritePictures.contains(picture);
	}

	public boolean isFavorite(ComicBook comic) {
		return favoriteComics.contains(comic);
	}

	private static <T> boolean toggleFavorite(@NotNull Collection<T> favorites, T entity) {
		if (favorites.contains(entity)) {
			favorites.remove(entity);
			return false;
		} else {
			favorites.add(entity);
			return true;
		}
	}

	public boolean toggleFavorite(Video video) {
		return toggleFavorite(favoriteVideos, video);
	}

	public boolean toggleFavorite(Picture picture) {
		return toggleFavorite(favoritePictures, picture);
	}

	public boolean toggleFavorite(ComicBook comicBook) {
		return toggleFavorite(favoriteComics, comicBook);
	}
}
