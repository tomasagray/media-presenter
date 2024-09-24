package net.tomasbot.mp.model;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
  @JdbcTypeCode(SqlTypes.VARCHAR)
  private UUID id;

  private String username;

  @ElementCollection @ToString.Exclude private Set<UUID> favoriteVideos;
  @ElementCollection @ToString.Exclude private Set<UUID> favoritePictures;
  @ElementCollection @ToString.Exclude private Set<UUID> favoriteComics;

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
