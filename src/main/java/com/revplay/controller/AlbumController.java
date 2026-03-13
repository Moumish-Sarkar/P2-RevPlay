package com.revplay.controller;

import com.revplay.dto.AlbumDTO;
import com.revplay.dto.ApiResponse;
import com.revplay.model.User;
import com.revplay.repository.UserRepository;
import com.revplay.service.AlbumService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * REST Controller for managing Albums.
 * Handles creating albums, fetching albums (and their respective songs),
 * and serving album cover art images.
 * Many endpoints here use @PreAuthorize("hasRole('ARTIST')") to restrict
 * listeners from modifying data.
 */
@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    private final AlbumService albumService;
    private final UserRepository userRepository;

    public AlbumController(AlbumService albumService, UserRepository userRepository) {
        this.albumService = albumService;
        this.userRepository = userRepository;
    }

    /**
     * Public endpoint to fetch all albums available in the platform.
     */
    @GetMapping
    public ResponseEntity<?> getAllAlbums() {
        try {
            List<AlbumDTO> albums = albumService.getAllAlbums();
            return ResponseEntity.ok(new ApiResponse(true, "Albums retrieved", albums));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    // --- Public: Get single album with songs ---
    @GetMapping("/{id}")
    public ResponseEntity<?> getAlbum(@PathVariable Long id) {
        try {
            AlbumDTO album = albumService.getAlbum(id);
            return ResponseEntity.ok(new ApiResponse(true, "Album retrieved", album));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    // --- Artist: Get my albums ---
    @PreAuthorize("hasRole('ARTIST')")
    @GetMapping("/my")
    public ResponseEntity<?> getMyAlbums(Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            List<AlbumDTO> albums = albumService.getAlbumsByArtist(user.getId());
            return ResponseEntity.ok(new ApiResponse(true, "My albums retrieved", albums));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Endpoint for Artists to create a new album.
     * Takes multipart form data to allow uploading the cover image byte array
     * alongside the text details.
     */
    @PreAuthorize("hasRole('ARTIST')")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createAlbum(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "releaseDate", required = false) String releaseDate,
            @RequestParam(value = "coverArt", required = false) org.springframework.web.multipart.MultipartFile coverArtFile,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);

            if (name == null || name.trim().isEmpty()) {
                throw new RuntimeException("Album name is required");
            }

            AlbumDTO album = albumService.createAlbum(name.trim(), description, releaseDate, user, coverArtFile);
            return ResponseEntity.ok(new ApiResponse(true, "Album created", album));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    // --- Artist: Update album ---
    @PreAuthorize("hasRole('ARTIST')")
    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAlbum(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "releaseDate", required = false) String releaseDate,
            @RequestParam(value = "coverArt", required = false) org.springframework.web.multipart.MultipartFile coverArtFile,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);

            AlbumDTO album = albumService.updateAlbum(id, name, description, releaseDate, user, coverArtFile);
            return ResponseEntity.ok(new ApiResponse(true, "Album updated", album));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    // --- Artist: Delete album (only if empty) ---
    @PreAuthorize("hasRole('ARTIST')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlbum(@PathVariable Long id, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            albumService.deleteAlbum(id, user);
            return ResponseEntity.ok(new ApiResponse(true, "Album deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    // --- Artist: Add song to album ---
    @PreAuthorize("hasRole('ARTIST')")
    @PostMapping("/{id}/songs/{songId}")
    public ResponseEntity<?> addSongToAlbum(@PathVariable Long id, @PathVariable Long songId,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            AlbumDTO album = albumService.addSongToAlbum(id, songId, user);
            return ResponseEntity.ok(new ApiResponse(true, "Song added to album", album));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    // --- Artist: Remove song from album ---
    @PreAuthorize("hasRole('ARTIST')")
    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<?> removeSongFromAlbum(@PathVariable Long id, @PathVariable Long songId,
            Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            AlbumDTO album = albumService.removeSongFromAlbum(id, songId, user);
            return ResponseEntity.ok(new ApiResponse(true, "Song removed from album", album));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * Public endpoint for fetching an album's cover art.
     * Extracts the BLOB image bytes from the database and streams them back to the
     * frontend.
     */
    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getAlbumCover(@PathVariable Long id) {
        try {
            // Reusing getAlbum from AlbumService to verify existence,
            // but we need to fetch the raw entity to get the BLOB.
            // (Assuming albumRepository is accessible or we add a method to AlbumService)
            // It's cleaner to add a method to AlbumService. I will just rely on the
            // existing
            // findById if we can inject repository here or add a service method. Let's add
            // it to service.
            com.revplay.model.Album album = albumService.getAlbumEntityForCover(id);
            if (album.getCoverArtData() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, album.getCoverArtContentType())
                    .body(album.getCoverArtData());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
