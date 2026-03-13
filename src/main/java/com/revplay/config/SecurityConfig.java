package com.revplay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Main security configuration class for the application.
 * This class configures Spring Security, defining which endpoints are public,
 * which require authentication, and how authentication is handled (via JWT).
 * 
 * @EnableMethodSecurity allows us to use annotations like @PreAuthorize on
 *                       controller methods.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Configures the main security filter chain.
     * This defines the rules for all incoming HTTP requests.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS (Cross-Origin Resource Sharing) with our custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF (Cross-Site Request Forgery) protection since we are using
                // stateless JWTs, not session cookies
                .csrf(csrf -> csrf.disable())
                // Set session management to STATELESS because every request must include a JWT;
                // the server won't remember sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Define endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers("/api/auth/**").permitAll() // Login/Register endpoints
                        .requestMatchers(HttpMethod.GET, "/api/songs", "/api/songs/{id}", "/api/songs/search",
                                "/api/songs/filter", "/api/songs/genres", "/api/songs/top", "/api/songs/artist/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/songs/{id}/stream", "/api/songs/{id}/cover").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/albums", "/api/albums/{id}", "/api/albums/{id}/cover")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artists/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/playlists/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/playlists/{id}").permitAll()
                        // Static resources
                        .requestMatchers("/*.html", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .requestMatchers("/", "/index.html").permitAll()
                        // All other endpoints require the user to be authenticated (logged in with a
                        // valid JWT)
                        .anyRequest().authenticated())
                // Add our custom JwtFilter BEFORE the standard
                // UsernamePasswordAuthenticationFilter
                // This ensures our token is checked before Spring tries to authenticate the
                // user traditionally
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides the password encoder used to securely hash and verify user
     * passwords.
     * BCrypt is a strong hashing function.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager as a Bean so we can use it in our
     * AuthController
     * to authenticate users during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines exactly which origins, HTTP methods, and headers are allowed to make
     * cross-origin requests.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
