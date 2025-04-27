package net.tomasbot.mp.db;

import java.util.UUID;
import net.tomasbot.mp.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {}
