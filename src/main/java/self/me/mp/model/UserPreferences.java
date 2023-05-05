package self.me.mp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
public class UserPreferences {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
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

	public void toggleFavorite(Video video) {
		if (favoriteVideos.contains(video)) {
			favoriteVideos.remove(video);
		} else {
			favoriteVideos.add(video);
		}
	}

	public void toggleFavorite(Picture picture) {
		if (favoritePictures.contains(picture)) {
			favoritePictures.remove(picture);
		} else {
			favoritePictures.add(picture);
		}
	}

	public void toggleFavorite(ComicBook comicBook) {
		if (favoriteComics.contains(comicBook)) {
			favoriteComics.remove(comicBook);
		} else {
			favoriteComics.add(comicBook);
		}
	}
}
