package self.me.mp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
public class UserPreferences {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private UUID id;

	private String username;

	@OneToMany
	@Exclude
	private Set<Video> favoriteVideos;
	@OneToMany
	@Exclude
	private Set<Picture> favoritePictures;
	@OneToMany
	@Exclude
	private Set<ComicBook> favoriteComics;

	public UserPreferences(String username) {
		this.username = username;
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

  public boolean isFavorite(Video video) {
    return favoriteVideos.stream().map(Video::getId).anyMatch(id -> id.equals(video.getId()));
  }

  public boolean isFavorite(Picture picture) {
    return favoritePictures.contains(picture);
  }

  public boolean isFavorite(ComicBook comic) {
    return favoriteComics.contains(comic);
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o))
			return false;
		UserPreferences that = (UserPreferences) o;
		return getId() != null && Objects.equals(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
