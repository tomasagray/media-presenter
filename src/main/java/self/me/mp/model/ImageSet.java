package self.me.mp.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

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
@Indexed
public class ImageSet {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@ToString.Exclude
	private final Set<Image> images;

	@FullTextField
	private String title;

	@ManyToMany(targetEntity = Tag.class, fetch = FetchType.EAGER)
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private Set<Tag> tags;
	private final Timestamp added = Timestamp.from(Instant.now());

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
