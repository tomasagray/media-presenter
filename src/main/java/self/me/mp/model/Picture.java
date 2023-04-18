package self.me.mp.model;

import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.UUID;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Picture extends Image {

	@Builder(builderMethodName = "pictureBuilder")
	public Picture(UUID id, String title, int height, int width, long filesize, URI uri) {
		super(id, title, height, width, filesize, uri);
	}
}

