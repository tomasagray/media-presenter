package self.me.mp.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
public class ImageSet {

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@ToString.Exclude
	private final Set<Image> images;
	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	private UUID id;
	@ManyToMany
	private Set<Tag> tags;
	private final Timestamp added = Timestamp.from(Instant.now());
	private String title;

	public ImageSet() {
		this.images = new LinkedHashSet<>();
	}

	public void addImage(Image image) {
		this.images.add(image);
	}

	public Image getImage(UUID imageId) {
		return images.stream()
				.filter(img -> img.getId().equals(imageId))
				.findFirst()
				.orElse(null);
	}
}
