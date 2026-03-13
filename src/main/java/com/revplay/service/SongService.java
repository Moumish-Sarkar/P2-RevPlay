package com.revplay.service;

import com.revplay.dto.SongDTO;
import com.revplay.model.Song;
import com.revplay.model.User;
import com.revplay.model.Album;
import com.revplay.repository.SongRepository;
import com.revplay.repository.AlbumRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class handling all business logic related to Songs.
 * Controllers call these methods instead of talking to the Repositories
 * directly.
 * By using @Transactional, we ensure that if a complex operation fails halfway,
 * the entire database operation rolls back to prevent corrupted data.
 */
@Service
public class SongService {

    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;

    public SongService(SongRepository songRepository, AlbumRepository albumRepository) {
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
    }

    /**
     * Uploads a new song.
     * Extracts byte data from the MultipartFiles and saves them directly into the
     * database as BLOBs.
     * Also handles linking the song to the currently logged-in Artist and an
     * optional Album.
     * 
     * @param audioFile    The actual mp3/audio file.
     * @param coverArtFile Optional image file for the song cover.
     * @return A SongDTO containing the metadata and URLs to access the newly
     *         created song.
     */
    @Transactional
    public SongDTO uploadSong(MultipartFile audioFile, MultipartFile coverArtFile,
            String title, String genre, Integer duration,
            String visibility, Long albumId, User artist) throws IOException {

        Song song = new Song();
        song.setTitle(title);
        song.setGenre(genre);
        song.setDuration(duration);
        song.setArtist(artist);

        if (visibility != null && !visibility.isBlank()) {
            song.setVisibility(Song.Visibility.valueOf(visibility.toUpperCase()));
        }

        if (albumId != null) {
            Album album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new RuntimeException("Album not found with id: " + albumId));
            song.setAlbum(album);
        }

        // Store audio binary data
        song.setAudioData(audioFile.getBytes());
        song.setAudioContentType(audioFile.getContentType());

        // Store cover art binary data (optional)
        if (coverArtFile != null && !coverArtFile.isEmpty()) {
            song.setCoverArtData(coverArtFile.getBytes());
            song.setCoverArtContentType(coverArtFile.getContentType());
        }

        Song saved = songRepository.save(song);
        return toDTO(saved);
    }

    /**
     * Fetches all public songs as lightweight DTOs.
     * This specifically avoids pulling the massive audio blob bytes from the
     * database,
     * which makes the homepage load instantly instead of transferring hundreds of
     * megabytes.
     */
    public List<SongDTO> getAllPublicSongs() {
        return songRepository.findAllPublicWithoutBlob()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get song metadata by ID (lightweight).
     */
    public SongDTO getSongById(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found with id: " + id));
        return toDTO(song);
    }

    /**
     * Get the full Song entity (with BLOB data) for streaming.
     */
    @Transactional(readOnly = true)
    public Song getSongEntityForStreaming(Long id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found with id: " + id));
    }

    /**
     * Search songs by title or genre (lightweight).
     */
    public List<SongDTO> searchSongs(String query) {
        return songRepository.searchSongsWithoutBlob(query)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get top songs by play count (lightweight).
     */
    public List<SongDTO> getTopSongs() {
        return songRepository.findTopSongsWithoutBlob()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get songs by artist (lightweight).
     */
    public List<SongDTO> getSongsByArtist(Long artistId) {
        return songRepository.findByArtistIdWithoutBlob(artistId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Get all distinct genres.
     */
    public List<String> getAllGenres() {
        return songRepository.findAllGenres();
    }

    /**
     * Increment play count.
     */
    @Transactional
    public void incrementPlayCount(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found with id: " + songId));
        song.setPlayCount(song.getPlayCount() + 1);
        songRepository.save(song);
    }

    /**
     * Delete a song (owner-only check should be done in controller).
     */
    @Transactional
    public void deleteSong(Long id) {
        if (!songRepository.existsById(id)) {
            throw new RuntimeException("Song not found with id: " + id);
        }
        songRepository.deleteById(id);
    }

    /**
     * Converts a raw Song database entity into a lightweight SongDTO.
     * Critically, this NEVER includes the raw byte[] arrays. Instead, it generates
     * `/api/songs/{id}/stream` and `/api/songs/{id}/cover` URLs that the browser
     * can use to fetch the media files only when they are actually needed.
     */
    public SongDTO toDTO(Song song) {
        SongDTO dto = new SongDTO();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setGenre(song.getGenre());
        dto.setDuration(song.getDuration());
        dto.setAudioContentType(song.getAudioContentType());
        dto.setVisibility(song.getVisibility() != null ? song.getVisibility().name() : null);
        dto.setPlayCount(song.getPlayCount());

        // Build streaming and cover art URLs
        if (song.getId() != null) {
            dto.setAudioUrl("/api/songs/" + song.getId() + "/stream");
            if (song.getCoverArtContentType() != null) {
                dto.setCoverArtUrl("/api/songs/" + song.getId() + "/cover");
            }
        }

        // Artist info
        if (song.getArtist() != null) {
            dto.setArtistId(song.getArtist().getId());
            dto.setArtistName(song.getArtist().getDisplayName() != null
                    ? song.getArtist().getDisplayName()
                    : song.getArtist().getUsername());
        }

        // Album info
        if (song.getAlbum() != null) {
            dto.setAlbumId(song.getAlbum().getId());
            dto.setAlbumName(song.getAlbum().getName());
        }

        dto.setCreatedAt(song.getCreatedAt() != null ? song.getCreatedAt().toString() : null);
        return dto;
    }
}
