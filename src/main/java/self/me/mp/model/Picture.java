package self.me.mp.model;

import jakarta.persistence.Entity;
import lombok.*;
import org.hibernate.Hibernate;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
public class Picture extends Image {

	@Builder(builderMethodName = "pictureBuilder")
	public Picture(UUID id, String title, int height, int width, long filesize, URI uri) {
		super(id, title, height, width, filesize, uri);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o))
			return false;
		Picture picture = (Picture) o;
		return getId() != null && Objects.equals(getId(), picture.getId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}

