package net.tomasbot.mp.db;

import net.tomasbot.mp.model.RandomComicBookCollection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RandomComicCollectionRepo extends JpaRepository<RandomComicBookCollection, Long> {

  @Query("SELECT rc FROM RandomComicBookCollection rc ORDER BY rand()")
  List<RandomComicBookCollection> findRandom(PageRequest request);
}
