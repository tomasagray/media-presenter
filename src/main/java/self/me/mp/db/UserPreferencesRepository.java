package self.me.mp.db;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.mp.model.UserPreferences;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {

  Optional<UserPreferences> findByUsername(String username);
}
