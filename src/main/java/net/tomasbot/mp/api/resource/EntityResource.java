package net.tomasbot.mp.api.resource;

import java.util.Collection;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.tomasbot.mp.model.Tag;
import org.springframework.hateoas.RepresentationModel;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class EntityResource<T> extends RepresentationModel<EntityResource<T>> {

  private UUID id;
  private String title;
  private boolean favorite;
  private Collection<Tag> tags;
}
