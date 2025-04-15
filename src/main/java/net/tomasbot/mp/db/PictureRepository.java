package net.tomasbot.mp.db;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import net.tomasbot.mp.model.Picture;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PictureRepository extends JpaRepository<Picture, UUID> {

  @Query("SELECT p FROM Picture p ORDER BY p.added DESC")
  Page<Picture> findLatest(Pageable request);

  @Query("SELECT p FROM Picture p ORDER BY rand()")
  List<Picture> findRandom(Pageable request);

  @Query("SELECT p FROM Picture p WHERE p.width = 0")
  List<Picture> findUnprocessedPictures();

  @Query("SELECT p FROM Picture p WHERE p.uri = :uri")
  List<Picture> findAllByUri(@NotNull URI uri);
}
