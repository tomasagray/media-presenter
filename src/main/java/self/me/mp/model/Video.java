package self.me.mp.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import self.me.mp.db.converter.FFmpegMetadataConverter;
import self.me.mp.db.converter.PathConverter;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode
@RequiredArgsConstructor
@Entity
@Indexed
public class Video {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@FullTextField
	private final String title;
	private final Timestamp added = Timestamp.from(Instant.now());

	@ManyToMany
	@IndexedEmbedded
	@IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
	private Set<Tag> tags;

	@Convert(converter = PathConverter.class)
	private final Path file;

	@OneToOne(cascade = CascadeType.ALL)
	private final ImageSet thumbnails = new ImageSet();

	@Convert(converter = FFmpegMetadataConverter.class)
	@Column(columnDefinition = "LONGTEXT")
	private FFmpegMetadata metadata;

	public Video() {
		this.title = null;
		this.file = null;
	}

	public void addThumbnail(Image thumbnail) {
		this.thumbnails.addImage(thumbnail);
	}

	public String toString() {
		return String.format("[ID=%s, title=%s, file=%s, added=%s]",
				getId(), getTitle(), getFile(), getAdded());
	}
}
