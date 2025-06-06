package net.tomasbot.mp.model;

import jakarta.persistence.*;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import net.tomasbot.mp.db.converter.UriConverter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@Entity
@Indexed
public class Image implements Editable {

  @ManyToMany(fetch = FetchType.EAGER)
  @IndexedEmbedded
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  private final Set<Tag> tags = new HashSet<>();

  private final Timestamp added = Timestamp.from(Instant.now());

  @Column(columnDefinition = "LONGTEXT")
  @Convert(converter = UriConverter.class)
  private final URI uri;

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @JdbcTypeCode(SqlTypes.VARCHAR)
  private UUID id;

  @FullTextField private String title;
  private int height;
  private int width;
  private long filesize;

  public Image() {
    this.uri = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Image image)) return false;
    return getHeight() == image.getHeight()
        && getWidth() == image.getWidth()
        && getFilesize() == image.getFilesize()
        && Objects.equals(getId(), image.getId())
        && Objects.equals(getTags(), image.getTags())
        && Objects.equals(getAdded(), image.getAdded())
        && Objects.equals(getTitle(), image.getTitle())
        && Objects.equals(getUri(), image.getUri());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(),
        getTags(),
        getAdded(),
        getTitle(),
        getHeight(),
        getWidth(),
        getFilesize(),
        getUri());
  }
}
