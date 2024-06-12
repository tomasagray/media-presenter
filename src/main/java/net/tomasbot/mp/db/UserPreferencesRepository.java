package net.tomasbot.mp.db;

import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

  Optional<UserPreferences> findByUsername(String username);
}
