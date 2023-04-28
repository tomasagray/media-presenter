package self.me.mp.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.mp.model.Image;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

	Optional<Image> findByUri(URI uri);
}
