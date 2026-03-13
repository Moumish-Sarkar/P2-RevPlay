package com.revplay.controller;

import com.revplay.dto.ApiResponse;
import com.revplay.dto.PlaylistDTO;
import com.revplay.model.User;
import com.revplay.repository.UserRepository;
import com.revplay.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing User Playlists.
 * Unlike Albums (which are created by Artists), Playlists can be created by ANY
 * logged-in user.
 * This controller handles creating playlists, fetching them, and
 * adding/removing songs from them.
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final UserRepository userRepository;

    public PlaylistController(PlaylistService playlistService, UserRepository userRepository) {
        this.playlistService = playlistService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyPlaylists(Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            List<PlaylistDTO> playlists = playlistService.getUserPlaylists(user.getId());
            return ResponseEntity.ok(new ApiResponse(true, "Playlists retrieved", playlists));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlaylist(@PathVariable Long id) {
        try {
            PlaylistDTO playlist = playlistService.getPlaylistById(id);
            return ResponseEntity.ok(new ApiResponse(true, "Playlist retrieved", playlist));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPlaylist(@RequestBody Map<String, Object> payload, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            Boolean isPublic = payload.containsKey("isPublic") ? (Boolean) payload.get("isPublic") : true;

            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("Playlist name is required");
            }

            PlaylistDTO playlist = playlistService.createPlaylist(name, description, isPublic, user);
            return ResponseEntity.ok(new ApiResponse(true, "Playlist created", playlist));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlaylist(@PathVariable Long id, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            playlistService.deletePlaylist(id, user);
            return ResponseEntity.ok(new ApiResponse(true, "Playlist deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/{id}/songs/{songId}")
    public ResponseEntity<?> addSongToPlaylist(@PathVariable Long id, @PathVariable Long songId,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            playlistService.addSongToPlaylist(id, songId, user);
            return ResponseEntity.ok(new ApiResponse(true, "Song added to playlist", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<?> removeSongFromPlaylist(@PathVariable Long id, @PathVariable Long songId,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            playlistService.removeSongFromPlaylist(id, songId, user);
            return ResponseEntity.ok(new ApiResponse(true, "Song removed from playlist", null));
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
