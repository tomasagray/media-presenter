package self.me.mp.plugin.ffmpeg.metadata;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
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
public class FFmpegStream {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ffmpeg_stream_generator")
  private Long id;

  private int index;
  private String codec_name;
  private String codec_long_name;
  private String profile;
  private CodecType codec_type;
  private String codec_time_base;
  private String codec_tag_string;
  private String codec_tag;
  private int width, height;
  private int has_b_frames;
  private String sample_aspect_ratio;
  private String display_aspect_ratio;
  private String pix_fmt;
  private int level;
  private String chroma_location;
  private int refs;
  private String is_avc;
  private String nal_length_size;
  private String r_frame_rate;
  private String avg_frame_rate;
  private String time_base;
  private long start_pts;
  private double start_time;
  private long duration_ts;
  private double duration;
  private long bit_rate;
  private long max_bit_rate;
  private int bits_per_raw_sample;
  private int bits_per_sample;
  private long nb_frames;
  private String sample_fmt;
  private int sample_rate;
  private int channels;
  private String channel_layout;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private FFmpegDisposition disposition;

  @ElementCollection private Map<String, String> tags;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    FFmpegStream that = (FFmpegStream) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  public enum CodecType {
    VIDEO,
    AUDIO
  }

  @Data
  @Entity
  public static class FFmpegDisposition {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ffmpeg_metadata_generator")
    private Long id;

    private int _default;
    private int dub;
    private int original;
    private int comment;
    private int lyrics;
    private int karaoke;
    private int forced;
    private int hearing_impaired;
    private int visual_impaired;
    private int clean_effects;
    private int attached_pic;
    private int captions;
    private int descriptions;
    private int metadata;
  }
}
