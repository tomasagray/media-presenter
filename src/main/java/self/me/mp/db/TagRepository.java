package self.me.mp.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.mp.model.Md5Id;
import self.me.mp.model.Tag;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Md5Id> {

	Optional<Tag> findByName(String name);

}
