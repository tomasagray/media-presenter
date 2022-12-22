package self.me.mp.db;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.mp.model.Video;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

  Page<Video> findAllByOrderByAddedDesc(Pageable pageable);
}
