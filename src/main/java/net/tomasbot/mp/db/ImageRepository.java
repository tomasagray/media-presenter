package net.tomasbot.mp.db;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import net.tomasbot.mp.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

  List<Image> findByUri(URI uri);
}
