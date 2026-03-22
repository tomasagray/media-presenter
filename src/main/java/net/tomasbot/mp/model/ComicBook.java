package net.tomasbot.mp.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
public class ComicBook extends ImageSet {

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

  @Override
  public String toString() {
    return String.format("[ID=%s, title=%s, added=%s]", getId(), getTitle(), getAdded());
  }
}
