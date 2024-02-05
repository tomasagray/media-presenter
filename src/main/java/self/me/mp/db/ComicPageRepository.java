package self.me.mp.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import self.me.mp.model.ComicPage;

import java.util.List;
import java.util.UUID;

public interface ComicPageRepository extends JpaRepository<ComicPage, UUID> {

	@Query("SELECT page FROM ComicPage page WHERE page NOT IN (SELECT cb.images FROM ComicBook cb)")
	List<ComicPage> findLoosePages();
}
