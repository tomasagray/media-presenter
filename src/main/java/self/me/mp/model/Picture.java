package self.me.mp.model;

import jakarta.persistence.Entity;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
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

