package self.me.mp.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import self.me.mp.api.service.user.UserService;
import self.me.mp.model.UserPreferences;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final Logger logger = LogManager.getLogger(SecurityConfig.class);
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
		UserDetails user = createUserIfNotExists(
				"user",
				passwordEncoder().encode("password"),
				Roles.USER);
		if (user != null) {
			detailsManager.createUser(user);
		}
		UserDetails admin = createUserIfNotExists(
				"admin",
				passwordEncoder().encode("password"),
				Roles.ADMIN);
		if (admin != null) {
			detailsManager.createUser(admin);
		}
	}

	public UserDetails createUserIfNotExists(String username, String password, Roles role) {
		try {
			UserPreferences preferences = userService.getUserPreferences(username);
			logger.info("Found existing User Preferences: {}", preferences);
			logger.info("User: {} already exists; skipping creation...", username);
			return null;
		} catch (IllegalStateException ignore) {
			logger.info("User: {} does not exist; creating...", username);
			return createNewUser(username, password, role);
		}
	}

	private @NotNull UserDetails createNewUser(
			@NotNull String username,
			@NotNull String password,
			@NotNull Roles role) {

		UserDetails user = User.withUsername(username)
				.password(password)
				.roles(role.name())
				.build();
		UserDetails preferences = userService.createUserPreferences(user);
		logger.info("Created User Preferences: {}", preferences);
		return user;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(@NotNull HttpSecurity http) throws Exception {
		return http.csrf()
				.disable().authorizeHttpRequests()
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
