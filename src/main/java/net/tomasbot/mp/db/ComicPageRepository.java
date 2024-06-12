package net.tomasbot.mp.db;

import java.util.List;
import java.util.UUID;
import net.tomasbot.mp.model.ComicPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComicPageRepository extends JpaRepository<ComicPage, UUID> {

  @Query("SELECT page FROM ComicPage page WHERE page NOT IN (SELECT cb.images FROM ComicBook cb)")
  List<ComicPage> findLoosePages();
}
