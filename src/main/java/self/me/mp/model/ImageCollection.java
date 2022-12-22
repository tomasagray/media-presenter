package self.me.mp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class ImageCollection {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_collection_generator")
  private Long id;

  @OneToMany(cascade = CascadeType.ALL)
  @ToString.Exclude
  private List<Image> images;

  private String title;

  public ImageCollection(List<Image> images, String title) {
    this.images = images;
    this.title = title;
  }
}
