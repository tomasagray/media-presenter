package self.me.mp.plugin.ffmpeg.metadata;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class FFmpegFormat {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ffmpeg_format_generator")
  private Long id;

  private String filename;
  private int nb_streams;
  private int nb_programs;
  private String format_name;
  private String format_long_name;
  private double start_time;
  private double duration;
  private long size;
  private long bit_rate;
  private int probe_score;
  @ElementCollection private Map<String, String> tags;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    FFmpegFormat that = (FFmpegFormat) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
