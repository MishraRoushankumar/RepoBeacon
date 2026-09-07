package com.repobeacon.backend.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.repobeacon.backend.security.GithubOAuth2UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final GithubOAuth2UserService gitHubOAuth2UserService;

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			AuthenticationSuccessHandler oauth2SuccessHandler,
			AuthenticationFailureHandler oauth2FailureHandler) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/login-url",
								"/oauth2/**",
								"/login/oauth2/**",
								"/error")
						.permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(
								new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.oauth2Login(oauth -> oauth
						.userInfoEndpoint(userInfo -> userInfo
								.userService(gitHubOAuth2UserService))
						.successHandler(oauth2SuccessHandler)
						.failureHandler(oauth2FailureHandler))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> response
								.setStatus(HttpStatus.NO_CONTENT.value()))
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("REPOBEACON_SESSION"));

		return http.build();
	}

	@Bean
	AuthenticationSuccessHandler oauth2SuccessHandler(
			@Value("${app.frontend-url}") String frontendUrl) {
		SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler() {
			@Override
			public void onAuthenticationSuccess(
					HttpServletRequest request,
					HttpServletResponse response,
					Authentication authentication) throws IOException, ServletException {
				HttpSession session = request.getSession(false);
				Authentication secContextAuth = SecurityContextHolder.getContext().getAuthentication();
				boolean hasSpringSecurityContext = session != null
						&& session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY) != null;

				log.info("[AUTH-DIAG][OAUTH-SUCCESS] requestedSessionId={}, httpSessionExists={}, sessionId={}, authClass={}, authPrincipalClass={}, authName={}, secContextAuth={}, sessionContainsSpringSecurityContext={}",
						request.getRequestedSessionId(),
						session != null,
						session != null ? session.getId() : null,
						authentication != null ? authentication.getClass().getName() : null,
						authentication != null && authentication.getPrincipal() != null
								? authentication.getPrincipal().getClass().getName()
								: null,
						authentication != null ? authentication.getName() : null,
						secContextAuth != null ? secContextAuth.getClass().getName() : null,
						hasSpringSecurityContext);

				super.onAuthenticationSuccess(request, response, authentication);
			}
		};
		handler.setDefaultTargetUrl(frontendUrl + "/auth/callback");
		return handler;
	}

	@Bean
	AuthenticationFailureHandler oauth2FailureHandler(
			@Value("${app.frontend-url}") String frontendUrl) {
		SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler();
		handler.setDefaultFailureUrl(frontendUrl + "/login?error=oauth_failed");
		return handler;
	}
}