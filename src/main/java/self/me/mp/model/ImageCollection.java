package self.me.mp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

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
  private final List<Image> images;

  private String title;

  public ImageCollection() {
    this.images = new ArrayList<>();
  }

  public ImageCollection(List<Image> images, String title) {
    this.images = images;
    this.title = title;
  }

  public void addImage(Image image) {
    this.images.add(image);
  }
}
