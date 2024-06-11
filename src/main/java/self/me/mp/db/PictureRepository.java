package self.me.mp.db;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import self.me.mp.model.Picture;

@Repository
public interface PictureRepository extends JpaRepository<Picture, UUID> {

  @Query("SELECT p FROM Picture p ORDER BY p.added DESC")
  Page<Picture> findLatest(Pageable request);

	@Query("SELECT p FROM Picture p ORDER BY rand()")
	List<Picture> findRandom(Pageable request);

	@Query("SELECT p FROM Picture p WHERE p.width = 0")
	List<Picture> findUnprocessedPictures();
}
