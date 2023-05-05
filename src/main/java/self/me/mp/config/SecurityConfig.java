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
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import self.me.mp.api.service.UserService;
import self.me.mp.model.UserPreferences;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final Logger logger = LogManager.getLogger(SecurityConfig.class);
	private final UserService userService;

	public SecurityConfig(UserService userService) {
		this.userService = userService;
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsManager() {
		// TODO: delete this, replace with JDBC implementation
		UserDetails user = createUser("user", Roles.USER);
		UserDetails admin = createUser("admin", Roles.ADMIN);
		return new InMemoryUserDetailsManager(user, admin);
	}

	public UserDetails createUser(String username, Roles role) {

		try {
			UserPreferences preferences = userService.getUserPreferences(username);
			logger.info("User: {} already exists; deleting...", username);
			userService.deleteUserPreferences(preferences.getId());
		} catch (IllegalStateException ignore) {
		}
		return createNewUser(username, role);
	}

	@NotNull
	private UserDetails createNewUser(@NotNull String username, @NotNull Roles role) {

		UserDetails user = User.withUsername(username)
				.password(passwordEncoder().encode("password"))
				.roles(role.name())
				.build();
		logger.info("Created User Preferences: {}", userService.createUserPreferences(user));
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
				.requestMatchers("/login*")
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
				.deleteCookies("JSESSIONID")
				.and()
				.build();
	}

	public enum Roles {
		USER,
		ADMIN,
	}
}
