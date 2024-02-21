package self.me.mp.model;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@ToString(callSuper = true)
@Indexed
@NoArgsConstructor
public class Performer extends Tag {
    public Performer(String name) {
        super(name);
    }
}
