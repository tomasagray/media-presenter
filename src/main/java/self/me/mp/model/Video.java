package self.me.mp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.net.URI;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Video {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(columnDefinition = "BINARY(16)")
  private UUID id;

  private String title;

  private URI uri;

  private Timestamp added;

  @OneToOne(cascade = CascadeType.ALL)
  private ImageCollection thumbnails;

  @OneToOne(cascade = CascadeType.ALL)
  private FFmpegMetadata metadata;
}
