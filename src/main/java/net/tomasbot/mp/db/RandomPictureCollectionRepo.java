package net.tomasbot.mp.db;

import net.tomasbot.mp.model.Picture;
import net.tomasbot.mp.model.RandomPictureCollection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RandomPictureCollectionRepo extends JpaRepository<RandomPictureCollection, Long> {

  @Query("SELECT rp FROM RandomPictureCollection rp ORDER BY rand()")
  List<RandomPictureCollection> findRandom(PageRequest request);

  @Query("SELECT rp FROM RandomPictureCollection rp WHERE :picture IN elements(rp.pictures)")
  List<RandomPictureCollection> findAllByPicture(Picture picture);
}
