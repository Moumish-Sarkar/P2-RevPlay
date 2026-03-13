package com.revplay.controller;

import com.revplay.dto.ApiResponse;
import com.revplay.dto.SongDTO;
import com.revplay.model.Song;
import com.revplay.model.User;
import com.revplay.repository.UserRepository;
import com.revplay.service.SongService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * REST Controller for managing Songs.
 * Exposes endpoints for uploading, deleting, and fetching songs.
 * Crucially, it provides endpoints for streaming the actual audio bytes to the
 * frontend audio player.
 */
@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService songService;
    private final UserRepository userRepository;

    public SongController(SongService songService, UserRepository userRepository) {
        this.songService = songService;
        this.userRepository = userRepository;
    }

    /**
     * Endpoint for artists to upload a new song.
     * Takes multipart form data (the audio file and optional cover art) instead of
     * raw JSON.
     * @PreAuthorize("hasRole('ARTIST')") ensures only Artists can use this
     * endpoint.
     */
    @PreAuthorize("hasRole('ARTIST')")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadSong(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(value = "coverArt", required = false) MultipartFile coverArt,
            @RequestParam("title") String title,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam(value = "visibility", required = false) String visibility,
            @RequestParam(value = "albumId", required = false) Long albumId,
            Authentication authentication) {

        try {
            User artist = getAuthenticatedUser(authentication);

            // Validate audio file
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Audio file is required"));
            }

            SongDTO songDTO = songService.uploadSong(
                    audioFile, coverArt, title, genre, duration, visibility, albumId, artist);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Song uploaded successfully", songDTO));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Failed to upload song: " + e.getMessage()));
        }
    }

    // ========================
    // List all public songs
    // ========================
    @GetMapping
    public ResponseEntity<?> getAllSongs() {
        List<SongDTO> songs = songService.getAllPublicSongs();
        return ResponseEntity.ok(new ApiResponse(true, "Songs retrieved", songs));
    }

    // ========================
    // Get song metadata by ID
    // ========================
    @GetMapping("/{id}")
    public ResponseEntity<?> getSongById(@PathVariable Long id) {
        try {
            SongDTO song = songService.getSongById(id);
            return ResponseEntity.ok(new ApiResponse(true, "Song retrieved", song));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Endpoint for streaming the actual audio file.
     * When the frontend `<audio>` player requests playback, it hits this URL.
     * The controller fetches the byte array from the database and returns it with
     * the correct content-type header (e.g., audio/mpeg).
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<byte[]> streamSong(@PathVariable Long id) {
        try {
            Song song = songService.getSongEntityForStreaming(id);

            if (song.getAudioData() == null) {
                return ResponseEntity.notFound().build();
            }

            // Increment play count
            songService.incrementPlayCount(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    song.getAudioContentType() != null ? song.getAudioContentType() : "audio/mpeg"));
            headers.setContentLength(song.getAudioData().length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + song.getTitle() + "\"");

            return new ResponseEntity<>(song.getAudioData(), headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint for fetching the song's cover art image.
     * Similar to the stream endpoint, it returns raw image bytes with an image/jpeg
     * content-type header.
     */
    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getCoverArt(@PathVariable Long id) {
        try {
            Song song = songService.getSongEntityForStreaming(id);

            if (song.getCoverArtData() == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    song.getCoverArtContentType() != null ? song.getCoverArtContentType() : "image/jpeg"));
            headers.setContentLength(song.getCoverArtData().length);

            return new ResponseEntity<>(song.getCoverArtData(), headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================
    // Search songs
    // ========================
    @GetMapping("/search")
    public ResponseEntity<?> searchSongs(@RequestParam("query") String query) {
        List<SongDTO> songs = songService.searchSongs(query);
        return ResponseEntity.ok(new ApiResponse(true, "Search results", songs));
    }

    // ========================
    // Get top songs
    // ========================
    @GetMapping("/top")
    public ResponseEntity<?> getTopSongs() {
        List<SongDTO> songs = songService.getTopSongs();
        return ResponseEntity.ok(new ApiResponse(true, "Top songs retrieved", songs));
    }

    // ========================
    // Get all genres
    // ========================
    @GetMapping("/genres")
    public ResponseEntity<?> getGenres() {
        List<String> genres = songService.getAllGenres();
        return ResponseEntity.ok(new ApiResponse(true, "Genres retrieved", genres));
    }

    // ========================
    // Get songs by artist
    // ========================
    @GetMapping("/artist/{artistId}")
    public ResponseEntity<?> getSongsByArtist(@PathVariable Long artistId) {
        List<SongDTO> songs = songService.getSongsByArtist(artistId);
        return ResponseEntity.ok(new ApiResponse(true, "Artist songs retrieved", songs));
    }

    // ========================
    // Delete a song (owner only)
    // ========================
    @PreAuthorize("hasRole('ARTIST')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSong(@PathVariable Long id, Authentication authentication) {
        try {
            User user = getAuthenticatedUser(authentication);
            SongDTO song = songService.getSongById(id);

            // Only the owner can delete
            if (!song.getArtistId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "You can only delete your own songs"));
            }

            songService.deleteSong(id);
            return ResponseEntity.ok(new ApiResponse(true, "Song deleted successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    // ========================
    // Helper: get authenticated User entity
    // ========================
    private User getAuthenticatedUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
