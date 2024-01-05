package self.me.mp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.type.SqlTypes;
import self.me.mp.db.converter.FFmpegMetadataConverter;
import self.me.mp.db.converter.PathConverter;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Indexed
public class Video {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private UUID id;

	@FullTextField
	private String title;
	private final Timestamp added = Timestamp.from(Instant.now());

	@ManyToMany(fetch = FetchType.EAGER)
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

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Video video))
			return false;
		return Objects.equals(getId(), video.getId()) && Objects.equals(getTitle(), video.getTitle())
				&& Objects.equals(getAdded(), video.getAdded()) && Objects.equals(getTags(),
				video.getTags()) && Objects.equals(getFile(), video.getFile()) && Objects.equals(
				getThumbnails(), video.getThumbnails()) && Objects.equals(getMetadata(),
				video.getMetadata());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getTitle(), getAdded(), getTags(), getFile(), getThumbnails(),
				getMetadata());
	}
}
