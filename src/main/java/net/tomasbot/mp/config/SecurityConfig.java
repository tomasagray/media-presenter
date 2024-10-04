package net.tomasbot.mp.config;

import static org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.sql.DataSource;
import net.tomasbot.mp.api.service.user.UserPreferenceService;
import net.tomasbot.mp.model.UserPreferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final Logger LOGGER = LogManager.getLogger(SecurityConfig.class);

  private final UserPreferenceService userPreferenceService;

  @Value("#{environment.REMEMBER_ME_KEY}")
  private String REMEMBER_ME_KEY;

  public SecurityConfig(UserPreferenceService userPreferenceService) {
    this.userPreferenceService = userPreferenceService;
  }

  @Bean
  public UserDetailsManager userDetailsManager(DataSource dataSource) {
    JdbcUserDetailsManager detailsManager = new JdbcUserDetailsManager(dataSource);
    createDefaultUsers(detailsManager);
    return detailsManager;
  }

  private void createDefaultUsers(@NotNull UserDetailsManager detailsManager) {
    // TODO: implement new user registration
    UserDetails user =
        createUserIfNotExists("user", passwordEncoder().encode("password"), Roles.USER);
    if (user != null) {
      detailsManager.createUser(user);
    }
    UserDetails admin =
        createUserIfNotExists("admin", passwordEncoder().encode("password"), Roles.ADMIN);
    if (admin != null) {
      detailsManager.createUser(admin);
    }
  }

  private @Nullable UserDetails createUserIfNotExists(
      String username, String password, Roles role) {
    try {
      UserPreferences preferences = userPreferenceService.getUserPreferences(username);
      LOGGER.info("Found existing User Preferences: {}", preferences);
      LOGGER.info("User: {} already exists; skipping creation...", username);
      return null;
    } catch (IllegalArgumentException e) {
      LOGGER.info("User: {} does not exist; creating...", username);
      return createNewUser(username, password, role);
    }
  }

  private @NotNull UserDetails createNewUser(
      @NotNull String username, @NotNull String password, @NotNull Roles role) {
    UserDetails user = User.withUsername(username).password(password).roles(role.name()).build();
    UserDetails preferences = userPreferenceService.createUserPreferences(user);
    LOGGER.info("Created User Preferences: {}", preferences);
    return user;
  }

  @Bean
  AuthenticationFailureHandler getFailureHandler() {
    return new SimpleUrlAuthenticationFailureHandler() {
      @Override
      public void onAuthenticationFailure(
          HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
          throws IOException, ServletException {
        String[] username = request.getParameterMap().get("username");
        LOGGER.error(
            "Login with (username={}, password=*****) failed; {}", username, e.getMessage());
        request.getRequestDispatcher("/login?error=true").forward(request, response);
      }
    };
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
    RememberMeTokenAlgorithm encodingAlgorithm = RememberMeTokenAlgorithm.SHA256;
    TokenBasedRememberMeServices rememberMe =
        new TokenBasedRememberMeServices(REMEMBER_ME_KEY, userDetailsService, encodingAlgorithm);
    rememberMe.setMatchingAlgorithm(RememberMeTokenAlgorithm.MD5);
    return rememberMe;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, RememberMeServices rememberMeServices)
      throws Exception {
    return http.csrf()
        .disable()
        .authorizeHttpRequests()
        .requestMatchers("/admin/**")
        .hasRole(Roles.ADMIN.name())
        .requestMatchers("/anonymous*")
        .anonymous()
        .requestMatchers("/login*", "/logout*", "/css/**", "/img/**", "/js/**")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .rememberMe(remember -> remember.rememberMeServices(rememberMeServices))
        .formLogin()
        .loginPage("/login")
        .loginProcessingUrl("/request_login")
        .defaultSuccessUrl("/home", true)
        .failureUrl("/login?error=true")
        .failureHandler(getFailureHandler())
        .and()
        .logout()
        .logoutUrl("/logout")
        .logoutSuccessUrl("/logout_success")
        .deleteCookies("JSESSIONID")
        .and()
        .build();
  }

  public enum Roles {
    USER,
    ADMIN,
  }
}
