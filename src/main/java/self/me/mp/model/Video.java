package self.me.mp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import self.me.mp.db.converter.PathConverter;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Video {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	private UUID id;
	private final String title;

	@OneToMany
	private final Set<Tag> tags = new HashSet<>();

	@Convert(converter = PathConverter.class)
	private final Path file;
	private final Timestamp added = Timestamp.from(Instant.now());

	@OneToOne(cascade = CascadeType.ALL)
	private final ImageCollection thumbnails = new ImageCollection();

	@OneToOne(cascade = CascadeType.ALL)
	private FFmpegMetadata metadata;

	public Video() {
		this.title = null;
		this.file = null;
	}

	public void addThumbnail(Image thumbnail) {
		this.thumbnails.addImage(thumbnail);
	}
}
