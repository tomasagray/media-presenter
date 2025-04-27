package net.tomasbot.mp.db;

import java.util.List;
import java.util.Optional;
import net.tomasbot.mp.model.Md5Id;
import net.tomasbot.mp.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Md5Id> {

  Optional<Tag> findByNameIgnoreCase(String name);

  @Query("SELECT count(v) FROM Video v WHERE :tag in elements(v.tags)")
  Integer findVideoReferenceCount(Tag tag);

  @Query("SELECT count(i) FROM Image i WHERE :tag in elements(i.tags)")
  Integer findImageReferenceCount(Tag tag);

  @Query("SELECT count(c) FROM ImageSet c WHERE :tag in elements(c.tags)")
  Integer findImageSetReferenceCount(Tag tag);

  @Query("SELECT t FROM Tag t WHERE t.name LIKE :name ORDER BY t.referenceCount DESC")
  List<Tag> findTagsByNameStartingWith(String name);
}
