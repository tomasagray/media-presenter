package net.tomasbot.mp.db;

import net.tomasbot.mp.model.Favorite;
import net.tomasbot.mp.model.UserPreferences;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

  Optional<UserPreferences> findByUsername(String username);

  @Query("""
          SELECT fv
          FROM UserPreferences up
          JOIN up.favoriteVideos fv
          WHERE up.username = :username
          ORDER BY fv.timestamp DESC
          """)
  Page<Favorite> getUserVideoFavorites(String username, Pageable request);

  @Query("""
          SELECT fp
          FROM UserPreferences up
          JOIN up.favoritePictures fp
          WHERE up.username = :username
          ORDER BY fp.timestamp
          """)
  Page<Favorite> getUserPictureFavorites(String username, Pageable request);

  @Query("""
          SELECT fc
          FROM UserPreferences up
          JOIN up.favoriteComics fc
          WHERE up.username = :username
          ORDER BY fc.timestamp DESC
          """)
  Page<Favorite> getUserComicFavorites(String username, Pageable request);
}
