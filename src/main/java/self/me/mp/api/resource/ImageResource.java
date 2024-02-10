package self.me.mp.api.resource;

import lombok.Data;
import org.springframework.hateoas.RepresentationModel;
import self.me.mp.model.Tag;

import java.util.Collection;
import java.util.UUID;

@Data
public abstract class ImageResource<T> extends RepresentationModel<ImageResource<T>> {

    private UUID id;
    private String title;
    private Collection<Tag> tags;
    private boolean favorite;

}
