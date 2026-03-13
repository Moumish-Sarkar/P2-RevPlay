package com.revplay.controller;

import com.revplay.dto.ApiResponse;
import com.revplay.dto.AuthResponse;
import com.revplay.dto.LoginRequest;
import com.revplay.dto.RegisterRequest;
import com.revplay.model.ArtistProfile;
import com.revplay.model.User;
import com.revplay.repository.ArtistProfileRepository;
import com.revplay.repository.UserRepository;
import com.revplay.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsible for user authentication and registration.
 * These endpoints are public (no token required) and they are responsible
 * for verifying credentials and issuing JWT tokens to valid users.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final ArtistProfileRepository artistProfileRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;

        public AuthController(AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        ArtistProfileRepository artistProfileRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil) {
                this.authenticationManager = authenticationManager;
                this.userRepository = userRepository;
                this.artistProfileRepository = artistProfileRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtUtil = jwtUtil;
        }

        /**
         * Endpoint for registering a new user.
         * Validates the request, ensures the username/email aren't already taken,
         * hashes the password securely, saves the user to the database, and finally
         * delegates a JWT token so the user is instantly logged in after registering.
         * 
         * @param request The JSON body containing registration details (username,
         *                password, email, role).
         * @return An ApiResponse containing the newly created user data and their JWT
         *         token.
         */
        @PostMapping("/register")
        public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
                try {
                        // Check if username already exists
                        if (userRepository.existsByUsername(request.getUsername())) {
                                return ResponseEntity.badRequest()
                                                .body(new ApiResponse(false, "Username is already taken"));
                        }

                        // Check if email already exists
                        if (userRepository.existsByEmail(request.getEmail())) {
                                return ResponseEntity.badRequest()
                                                .body(new ApiResponse(false, "Email is already registered"));
                        }

                        // Create new user
                        User user = new User();
                        user.setUsername(request.getUsername());
                        user.setEmail(request.getEmail());
                        user.setPassword(passwordEncoder.encode(request.getPassword()));
                        user.setDisplayName(request.getDisplayName() != null
                                        ? request.getDisplayName()
                                        : request.getUsername());

                        // Set role
                        if ("ARTIST".equalsIgnoreCase(request.getRole())) {
                                user.setRole(User.Role.ARTIST);
                        } else {
                                user.setRole(User.Role.USER);
                        }

                        User savedUser = userRepository.save(user);

                        // Create artist profile if role is ARTIST
                        if (savedUser.getRole() == User.Role.ARTIST) {
                                ArtistProfile profile = new ArtistProfile();
                                profile.setUser(savedUser);
                                profile.setArtistName(request.getArtistName() != null
                                                ? request.getArtistName()
                                                : savedUser.getDisplayName());
                                artistProfileRepository.save(profile);
                        }

                        // Generate JWT token
                        String token = jwtUtil.generateToken(
                                        savedUser.getUsername(),
                                        savedUser.getRole().name(),
                                        savedUser.getId());

                        AuthResponse authResponse = new AuthResponse(
                                        token,
                                        savedUser.getUsername(),
                                        savedUser.getEmail(),
                                        savedUser.getRole().name(),
                                        savedUser.getId(),
                                        savedUser.getDisplayName(),
                                        savedUser.getProfilePicture());

                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(new ApiResponse(true, "Registration successful", authResponse));

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ApiResponse(false, "Registration failed: " + e.getMessage()));
                }
        }

        /**
         * Endpoint for logging in an existing user.
         * Takes the credentials, attempts to authenticate them against the database via
         * Spring Security.
         * If successful, it generates and returns a new JWT token.
         * 
         * @param request The JSON body containing the username/email and password.
         * @return An ApiResponse containing the user data and their JWT token.
         */
        @PostMapping("/login")
        public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
                try {
                        // Authenticate
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.getUsernameOrEmail(),
                                                        request.getPassword()));

                        // Find the user
                        User user = userRepository.findByUsernameOrEmail(
                                        request.getUsernameOrEmail(),
                                        request.getUsernameOrEmail())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        // Generate JWT token
                        String token = jwtUtil.generateToken(
                                        user.getUsername(),
                                        user.getRole().name(),
                                        user.getId());

                        AuthResponse authResponse = new AuthResponse(
                                        token,
                                        user.getUsername(),
                                        user.getEmail(),
                                        user.getRole().name(),
                                        user.getId(),
                                        user.getDisplayName(),
                                        user.getProfilePicture());

                        return ResponseEntity.ok(new ApiResponse(true, "Login successful", authResponse));

                } catch (AuthenticationException e) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(new ApiResponse(false, "Invalid username/email or password"));
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ApiResponse(false, "Login failed: " + e.getMessage()));
                }
        }
}
