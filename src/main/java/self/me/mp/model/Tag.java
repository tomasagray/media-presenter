package self.me.mp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.util.UUID;

@Data
@Entity
@Indexed
public class Tag {

	@FullTextField
	private final String name;
	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	private UUID tagId;

	public Tag() {
		this.name = null;
	}

	public Tag(String name) {
		this.name = name;
	}
}
