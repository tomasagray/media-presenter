package net.tomasbot.mp.db;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.model.ComicBook;
import net.tomasbot.mp.model.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ComicBookRepository extends JpaRepository<ComicBook, UUID> {

  @Query("SELECT cb FROM ComicBook cb ORDER BY cb.added DESC")
  Page<ComicBook> findLatest(Pageable request);

  @Query("SELECT cb FROM ComicBook cb ORDER BY rand()")
  List<ComicBook> findRandomComics(Pageable request);

  Optional<ComicBook> findComicBookByImagesContaining(Image image);

  @Query("SELECT cb FROM ComicBook cb WHERE cb.location = :directory")
  List<ComicBook> findComicBooksIn(Path directory);
}
