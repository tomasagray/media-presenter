package net.tomasbot.mp.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.sql.DataSource;
import net.tomasbot.mp.api.service.user.UserService;
import net.tomasbot.mp.model.UserPreferences;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final Logger LOGGER = LogManager.getLogger(SecurityConfig.class);

  private final UserService userService;

  public SecurityConfig(UserService userService) {
    this.userService = userService;
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

  public UserDetails createUserIfNotExists(String username, String password, Roles role) {
    try {
      UserPreferences preferences = userService.getUserPreferences(username);
      LOGGER.info("Found existing User Preferences: {}", preferences);
      LOGGER.info("User: {} already exists; skipping creation...", username);
      return null;
    } catch (IllegalStateException ignore) {
      LOGGER.info("User: {} does not exist; creating...", username);
      return createNewUser(username, password, role);
    }
  }

  private @NotNull UserDetails createNewUser(
      @NotNull String username, @NotNull String password, @NotNull Roles role) {

    UserDetails user = User.withUsername(username).password(password).roles(role.name()).build();
    UserDetails preferences = userService.createUserPreferences(user);
    LOGGER.info("Created User Preferences: {}", preferences);
    return user;
  }

  @Bean
  public AuthenticationFailureHandler getFailureHandler() {
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
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(@NotNull HttpSecurity http) throws Exception {
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
