package net.tomasbot.mp.db;

import net.tomasbot.mp.model.RandomVideoCollection;
import net.tomasbot.mp.model.Video;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RandomVideoCollectionRepo extends JpaRepository<RandomVideoCollection, Long> {

  @Query("SELECT rv FROM RandomVideoCollection rv ORDER BY rand()")
  List<RandomVideoCollection> findRandom(PageRequest request);

  @Query("SELECT rv FROM RandomVideoCollection rv WHERE :video IN elements(rv.videos)")
  List<RandomVideoCollection> findByVideo(@NotNull Video video);
}
