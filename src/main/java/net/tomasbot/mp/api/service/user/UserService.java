package net.tomasbot.mp.api.service.user;

import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.db.UserPreferencesRepository;
import net.tomasbot.mp.model.UserPreferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

  private final Logger logger = LogManager.getLogger(UserService.class);

  private final UserPreferencesRepository repository;

  public UserService(UserPreferencesRepository repository) {
    this.repository = repository;
  }

  public UserDetails createUserPreferences(@NotNull UserDetails user) {
    logger.info("Creating new User Preferences for: {}", user);
    UserPreferences preferences = new UserPreferences(user.getUsername());
    repository.save(preferences);
    return user;
  }

  public UserPreferences getUserPreferences() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    UserDetails user = (UserDetails) auth.getPrincipal();
    return getUserPreferences(user.getUsername());
  }

  public UserPreferences getUserPreferences(@NotNull String username) {
    logger.trace("Getting User Preferences for: {}", username);
    Optional<UserPreferences> optional = repository.findByUsername(username);
    if (optional.isEmpty()) {
      throw new IllegalStateException("Could not find UserPreferences for User: " + username);
    }
    return optional.get();
  }

  public void deleteUserPreferences(@NotNull UUID prefId) {
    repository.deleteById(prefId);
  }
}
