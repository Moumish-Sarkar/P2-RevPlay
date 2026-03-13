package com.revplay.controller;

import com.revplay.dto.ApiResponse;
import com.revplay.dto.FavoriteDTO;
import com.revplay.model.User;
import com.revplay.repository.UserRepository;
import com.revplay.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing User Favorites (Liked Songs).
 * Allows any authenticated user to toggle a song's favorite status, check if
 * they
 * have already favorited a song, and fetch their entire list of liked songs.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    @PostMapping("/toggle/{songId}")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long songId, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            boolean isAdded = favoriteService.toggleFavorite(songId, user);
            String message = isAdded ? "Added to favorites" : "Removed from favorites";
            return ResponseEntity.ok(new ApiResponse(true, message, isAdded));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/check/{songId}")
    public ResponseEntity<?> checkFavorite(@PathVariable Long songId, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            boolean isFavorite = favoriteService.isFavorite(songId, user);
            return ResponseEntity.ok(new ApiResponse(true, "Favorite status retrieved", isFavorite));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyFavorites(Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            List<FavoriteDTO> favorites = favoriteService.getUserFavorites(user.getId());
            return ResponseEntity.ok(new ApiResponse(true, "Favorites retrieved", favorites));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
