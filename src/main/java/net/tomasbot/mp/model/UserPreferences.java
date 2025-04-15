package net.tomasbot.mp.model;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
public class UserPreferences {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @JdbcTypeCode(SqlTypes.UUID)
  private UUID id;

  private String username;

  @OneToMany @ToString.Exclude private Set<Favorite> favoriteVideos;
  @OneToMany @ToString.Exclude private Set<Favorite> favoritePictures;
  @OneToMany @ToString.Exclude private Set<Favorite> favoriteComics;

  public UserPreferences(String username) {
    this.username = username;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    UserPreferences that = (UserPreferences) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
