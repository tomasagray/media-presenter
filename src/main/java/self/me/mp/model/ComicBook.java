package self.me.mp.model;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.Hibernate;
import self.me.mp.db.converter.PathConverter;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
public class ComicBook extends ImageSet {

	@Convert(converter = PathConverter.class)
	private Path location;

	public Image getImage(int i) {
		List<Image> images = new ArrayList<>(getImages());
		return images.get(i);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o))
			return false;
		ComicBook comicBook = (ComicBook) o;
		return getId() != null && Objects.equals(getId(), comicBook.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
