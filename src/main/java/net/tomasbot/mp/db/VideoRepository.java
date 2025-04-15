package net.tomasbot.mp.db;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.tomasbot.mp.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

  Page<Video> findAllByOrderByAddedDesc(Pageable pageable);

  @Query("SELECT v FROM Video v ORDER BY rand()")
  List<Video> findRandom(Pageable request);

  @Query("SELECT v FROM Video v WHERE v.title IS NULL")
  List<Video> findUnprocessedVideos();

  @Query("SELECT v FROM Video v WHERE v.file = :file")
  List<Video> findAllByFile(Path file);
}
