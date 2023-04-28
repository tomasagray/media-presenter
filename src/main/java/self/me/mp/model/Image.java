package self.me.mp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import self.me.mp.db.converter.UriConverter;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@Entity
public class Image {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	private UUID id;
	private String title;
	private final Timestamp added = Timestamp.from(Instant.now());

	@ManyToMany(fetch = FetchType.EAGER)
	private final Set<Tag> tags = new HashSet<>();

	private int height;
	private int width;
	private long filesize;

	@Column(columnDefinition = "LONGTEXT")
	@Convert(converter = UriConverter.class)
	private final URI uri;

	public Image() {
		this.uri = null;
	}
}
