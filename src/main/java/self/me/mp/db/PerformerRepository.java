package self.me.mp.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.mp.model.Md5Id;
import self.me.mp.model.Performer;

@Repository
public interface PerformerRepository extends JpaRepository<Performer, Md5Id> {

    Optional<Performer> findByName(String name);

}
