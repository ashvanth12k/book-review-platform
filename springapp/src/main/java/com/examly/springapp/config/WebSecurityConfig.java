package com.examly.springapp.config;

import com.examly.springapp.security.CustomUserDetailsService;
import com.examly.springapp.security.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Spring Security configuration.
 * - Uses SecurityFilterChain (NOT deprecated WebSecurityConfigurerAdapter).
 * - CORS is restricted to the configured FRONTEND_URL — no wildcard origins.
 * - JWT filter is stateless; sessions are never created.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService customUserDetailsService;

    /** Loaded from app.cors.allowed-origin (env-var: FRONTEND_URL). */
    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Autowired
    public WebSecurityConfig(JwtFilter jwtFilter,
                             CustomUserDetailsService customUserDetailsService) {
        this.jwtFilter = jwtFilter;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors()
            .and()
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // Return 401 (not 403) when the JWT is missing or invalid so the
            // frontend axios refresh-token interceptor can trigger a token refresh.
            .exceptionHandling()
                .authenticationEntryPoint(unauthorizedEntryPoint())
            .and()
            .authorizeRequests()

            // ── Public documentation ─────────────────────────────────
            .antMatchers(
                    "/v2/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/error"
            ).permitAll()

            // ── Public auth ──────────────────────────────────────────
            .antMatchers(HttpMethod.POST,
                    "/api/users/register",
                    "/api/users/login",
                    "/api/users/refresh-token")
            .permitAll()

            // ── Public book browsing ─────────────────────────────────
            .antMatchers(HttpMethod.GET,
                    "/api/books",
                    "/api/books/{id}",
                    "/api/books/search",
                    "/api/books/genre/**",
                    "/api/reviews")
            .permitAll()

            // ── Book management (requires PUBLISHER or ADMIN) ─────────
            .antMatchers(HttpMethod.POST,  "/api/books")
                .hasAnyRole("PUBLISHER", "ADMIN")
            .antMatchers(HttpMethod.PUT,   "/api/books/{id}")
                .hasAnyRole("PUBLISHER", "ADMIN")
            .antMatchers(HttpMethod.DELETE, "/api/books/{id}")
                .hasRole("ADMIN")

            // ── Ratings (any authenticated user) ─────────────────────
            .antMatchers(HttpMethod.POST, "/api/books/*/rating").authenticated()
            .antMatchers(HttpMethod.PUT,  "/api/books/*/rating").authenticated()

            // ── Reviews ───────────────────────────────────────────────
            .antMatchers(HttpMethod.POST,   "/api/reviews").authenticated()
            .antMatchers(HttpMethod.GET,    "/api/reviews/{id}").authenticated()
            .antMatchers(HttpMethod.PUT,    "/api/reviews/{id}").authenticated()
            .antMatchers(HttpMethod.DELETE, "/api/reviews/{id}").authenticated()

            // ── Users ─────────────────────────────────────────────────
            .antMatchers(HttpMethod.GET,  "/api/users").hasRole("ADMIN")
            .antMatchers("/api/users/profile", "/api/users/me").authenticated()
            .antMatchers(HttpMethod.GET,    "/api/users/{id}").hasRole("ADMIN")
            .antMatchers(HttpMethod.PUT,    "/api/users/{id}").authenticated()
            .antMatchers(HttpMethod.DELETE, "/api/users/{id}").hasRole("ADMIN")
            .antMatchers(HttpMethod.POST,
                    "/api/users/logout")
            .authenticated()
            .antMatchers(HttpMethod.POST, "/api/users/{id}/promote").hasRole("ADMIN")

            // ── Admin & Publisher portals ─────────────────────────────
            .antMatchers("/api/admin/**").hasRole("ADMIN")
            .antMatchers("/api/publisher/**").hasAnyRole("PUBLISHER", "ADMIN")

            // ── Recommendations ───────────────────────────────────────
            .antMatchers("/api/recommendations/**").authenticated()

            // ── Catch-all ─────────────────────────────────────────────
            .anyRequest().authenticated();

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Returns HTTP 401 whenever a request reaches a protected endpoint without
     * a valid JWT (token missing, expired, or tampered).
     * Without this, Spring Security defaults to 403 for anonymous principals,
     * which prevents the frontend refresh-token interceptor from firing.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized — invalid or missing token\"}");
        };
    }

    /** DaoAuthenticationProvider wires our UserDetailsService + PasswordEncoder. */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Exposes AuthenticationManager for programmatic use (e.g. login endpoint). */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * CORS configuration.
     * - Allowed origin is read from app.cors.allowed-origin (env-var FRONTEND_URL).
     * - No wildcard patterns — credentials are allowed only for the explicit origin.
     * - In dev, set FRONTEND_URL=http://localhost:3000 (via application-dev.properties).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Explicit origin only — wildcard + credentials is forbidden by spec and by our policy
        config.setAllowedOrigins(List.of(allowedOrigin));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}