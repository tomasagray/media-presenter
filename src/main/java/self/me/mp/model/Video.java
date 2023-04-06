package self.me.mp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

import java.net.URI;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Video {

	@OneToMany
	private final Set<Tag> tags = new HashSet<>();
	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	private UUID id;
	private String title;
	private URI uri;
	private Timestamp added;
	@OneToOne(cascade = CascadeType.ALL)
	private ImageCollection thumbnails;
	@OneToOne(cascade = CascadeType.ALL)
	private FFmpegMetadata metadata;
}
