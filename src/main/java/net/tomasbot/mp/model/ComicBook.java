package net.tomasbot.mp.model;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import java.nio.file.Path;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.tomasbot.mp.db.converter.PathConverter;
import org.hibernate.Hibernate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@SuperBuilder
@Entity
public class ComicBook extends ImageSet {

  @Convert(converter = PathConverter.class)
  private Path location;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    ComicBook comicBook = (ComicBook) o;
    return getId() != null && Objects.equals(getId(), comicBook.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
