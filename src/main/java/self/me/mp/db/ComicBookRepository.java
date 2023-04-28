package self.me.mp.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Image;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComicBookRepository extends JpaRepository<ComicBook, UUID> {

	@Query("SELECT cb FROM ComicBook cb ORDER BY cb.added")
	Page<ComicBook> findLatest(Pageable request);

	//	@Query("SELECT cb FROM ComicBook cb WHERE :image cb.images")
	Optional<ComicBook> findComicBookByImagesContaining(Image image);
}
