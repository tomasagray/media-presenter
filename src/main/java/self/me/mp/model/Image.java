package self.me.mp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString

@Builder
@AllArgsConstructor
@Entity
public class Image {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	private UUID id;
	private String title;

	@OneToMany
	private final Set<Tag> tags = new HashSet<>();

	private int height;
	private int width;
	private long filesize;
	private final URI uri;

	public Image() {
		this.uri = null;
	}
}
