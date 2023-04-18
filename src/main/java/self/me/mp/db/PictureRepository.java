package self.me.mp.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import self.me.mp.model.Picture;

import java.util.UUID;

@Repository
public interface PictureRepository extends JpaRepository<Picture, UUID> {

	@Query("SELECT p FROM Picture p ORDER BY p.added")
	Page<Picture> findLatest(Pageable request);

	@Query("SELECT p FROM Picture p ORDER BY rand()")
	Page<Picture> findRandom(Pageable request);
}
