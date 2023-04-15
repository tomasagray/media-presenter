package self.me.mp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class ImageSet {

  @OneToMany(cascade = CascadeType.ALL)
  @ToString.Exclude
  private final Set<Image> images;
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_set_generator")
  private Long id;

  private String title;

  public ImageSet() {
    this.images = new LinkedHashSet<>();
  }

  public void addImage(Image image) {
    this.images.add(image);
  }

  public Image getImage(UUID imageId) {
    return images.stream()
            .filter(img -> img.getId().equals(imageId))
            .findFirst()
            .orElse(null);
  }
}
