package net.tomasbot.mp.db;

import java.util.Optional;
import net.tomasbot.mp.model.Md5Id;
import net.tomasbot.mp.model.Performer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformerRepository extends JpaRepository<Performer, Md5Id> {

    Optional<Performer> findByName(String name);

}
