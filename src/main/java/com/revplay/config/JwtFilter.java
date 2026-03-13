package com.revplay.config;

import com.revplay.util.JwtUtil;
import com.revplay.repository.UserRepository;
import com.revplay.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that intercepts every incoming HTTP request to check for a valid JWT
 * token.
 * This class is a crucial part of the security layer. It extracts the token
 * from the
 * "Authorization" header, validates it, and sets the authenticated user in the
 * Spring Security Context so that the application knows who is making the
 * request.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    /**
     * Intercepts the request and checks for a valid JWT token.
     * 
     * @param request     The incoming HTTP request.
     * @param response    The outgoing HTTP response.
     * @param filterChain The chain of subsequent filters to execute.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extract the Authorization header
        String authHeader = request.getHeader("Authorization");

        // Check if the header exists and starts with "Bearer " (which indicates a JWT
        // token)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract the actual token string (ignoring the "Bearer " prefix)
            String token = authHeader.substring(7);

            // Validate the token to ensure it hasn't expired and hasn't been tampered with
            if (jwtUtil.validateToken(token)) {
                // Determine who the user is from the token
                String username = jwtUtil.getUsernameFromToken(token);
                // Extract their role to enforce authorizations (like
                // @PreAuthorize("hasRole('ARTIST')"))
                String role = jwtUtil.getRoleFromToken(token);

                // Create an authentication token containing the username and their granted
                // authorities
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));

                // Register the user with Spring's security context. This effectively "logs them
                // in" for this specific request.
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Continue the filter chain so the request can proceed to the Controller
        filterChain.doFilter(request, response);
    }
}
