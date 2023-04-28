package self.me.mp.model;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import self.me.mp.db.converter.PathConverter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Entity
public class ComicBook extends ImageSet {

	@Convert(converter = PathConverter.class)
	private Path location;

	public Image getImage(int i) {
		List<Image> images = new ArrayList<>(getImages());
		return images.get(i);
	}
}
