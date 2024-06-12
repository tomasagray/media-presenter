package net.tomasbot.mp.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.tomasbot.mp.db.Md5IdBridge;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@ToString
@Entity
@Indexed
public class Tag {

	@FullTextField
	protected final String name;

  @EmbeddedId
  @DocumentId(identifierBridge = @IdentifierBridgeRef(type = Md5IdBridge.class))
  @GenericGenerator(name = "tag_id_gen", strategy = "net.tomasbot.mp.db.Md5IdGenerator")
  @GeneratedValue(generator = "tag_id_gen")
  protected Md5Id tagId;

	public Tag() {
		this.name = null;
	}

	public Tag(@NotNull String name) {
		this.name = name.trim();
		if (this.name.isEmpty()) {
			throw new IllegalArgumentException("Empty Tag: " + name);
		}
		this.tagId = new Md5Id(this.name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		Tag tag = (Tag) o;
		return getTagId() != null && Objects.equals(getTagId(), tag.getTagId());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
