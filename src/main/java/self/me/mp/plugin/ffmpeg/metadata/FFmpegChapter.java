package self.me.mp.plugin.ffmpeg.metadata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class FFmpegChapter {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ffmpeg_chapter_generator")
  private Long chapterId;

  private String time_base;

  @Column(name = "chapter_start")
  private long start;

  private String start_time;

  @Column(name = "chapter_end")
  private long end;

  private String end_time;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    FFmpegChapter that = (FFmpegChapter) o;
    return chapterId != null && Objects.equals(chapterId, that.chapterId);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
