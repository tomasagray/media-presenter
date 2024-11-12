package net.tomasbot.mp.db;

import java.util.Optional;
import net.tomasbot.mp.model.Md5Id;
import net.tomasbot.mp.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Md5Id> {

  Optional<Tag> findByNameIgnoreCase(String name);
}
