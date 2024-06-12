package net.tomasbot.mp.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@ToString
@AllArgsConstructor
@SuperBuilder
@Entity
@Indexed
public class ImageSet {

  private final Timestamp added = Timestamp.from(Instant.now());

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private UUID id;
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@ToString.Exclude
	private Set<Image> images;
	@FullTextField
	private String title;
	@ManyToMany(targetEntity = Tag.class, fetch = FetchType.EAGER)
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private Set<Tag> tags;

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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ImageSet imageSet))
			return false;
		return Objects.equals(getId(), imageSet.getId()) && Objects.equals(getImages(),
				imageSet.getImages()) && Objects.equals(getTitle(), imageSet.getTitle()) && Objects.equals(
				getTags(), imageSet.getTags()) && Objects.equals(getAdded(), imageSet.getAdded());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getImages(), getTitle(), getTags(), getAdded());
	}
}
