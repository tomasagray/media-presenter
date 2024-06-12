package net.tomasbot.mp.api.resource;

import java.util.Collection;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.tomasbot.mp.model.Tag;
import org.springframework.hateoas.RepresentationModel;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ImageResource<T> extends RepresentationModel<ImageResource<T>> {

  private UUID id;
  private String title;
  private Collection<Tag> tags;
  private boolean favorite;
}
