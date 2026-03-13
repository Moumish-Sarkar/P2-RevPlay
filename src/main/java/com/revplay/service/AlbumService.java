package com.revplay.service;

import com.revplay.dto.AlbumDTO;
import com.revplay.dto.SongDTO;
import com.revplay.model.Album;
import com.revplay.model.Song;
import com.revplay.model.User;
import com.revplay.repository.AlbumRepository;
import com.revplay.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    public AlbumService(AlbumRepository albumRepository, SongRepository songRepository, SongService songService) {
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
        this.songService = songService;
    }

    /**
     * Create a new album (artist only).
     */
    @Transactional
    public AlbumDTO createAlbum(String name, String description, String releaseDate, User artist,
            org.springframework.web.multipart.MultipartFile coverArtFile) throws java.io.IOException {
        Album album = new Album();
        album.setName(name);
        album.setDescription(description);
        album.setArtist(artist);

        if (releaseDate != null && !releaseDate.isBlank()) {
            album.setReleaseDate(LocalDate.parse(releaseDate));
        }

        if (coverArtFile != null && !coverArtFile.isEmpty()) {
            album.setCoverArtData(coverArtFile.getBytes());
            album.setCoverArtContentType(coverArtFile.getContentType());
        }

        Album saved = albumRepository.save(album);
        return toDTO(saved, false);
    }

    /**
     * Get a single album with its songs.
     */
    public AlbumDTO getAlbum(Long id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album not found with id: " + id));
        return toDTO(album, true);
    }

    /**
     * Get the full Album entity (with BLOB data) for cover art.
     */
    @Transactional(readOnly = true)
    public Album getAlbumEntityForCover(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album not found with id: " + id));
    }

    /**
     * Get all albums (public — for listeners to browse).
     */
    public List<AlbumDTO> getAllAlbums() {
        return albumRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(a -> toDTO(a, false)).collect(Collectors.toList());
    }

    /**
     * Get albums by a specific artist.
     */
    public List<AlbumDTO> getAlbumsByArtist(Long artistId) {
        return albumRepository.findByArtistId(artistId)
                .stream().map(a -> toDTO(a, false)).collect(Collectors.toList());
    }

    /**
     * Update album info (artist only, own albums).
     */
    @Transactional
    public AlbumDTO updateAlbum(Long id, String name, String description, String releaseDate, User artist,
            org.springframework.web.multipart.MultipartFile coverArtFile) throws java.io.IOException {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album not found with id: " + id));

        if (!album.getArtist().getId().equals(artist.getId())) {
            throw new RuntimeException("You can only edit your own albums");
        }

        if (name != null && !name.isBlank())
            album.setName(name);
        if (description != null)
            album.setDescription(description);
        if (releaseDate != null && !releaseDate.isBlank()) {
            album.setReleaseDate(LocalDate.parse(releaseDate));
        }

        if (coverArtFile != null && !coverArtFile.isEmpty()) {
            album.setCoverArtData(coverArtFile.getBytes());
            album.setCoverArtContentType(coverArtFile.getContentType());
        }

        Album saved = albumRepository.save(album);
        return toDTO(saved, false);
    }

    /**
     * Delete an album — only if it has no songs.
     */
    @Transactional
    public void deleteAlbum(Long id, User artist) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Album not found with id: " + id));

        if (!album.getArtist().getId().equals(artist.getId())) {
            throw new RuntimeException("You can only delete your own albums");
        }

        List<Song> songs = songRepository.findByAlbumId(id);
        if (!songs.isEmpty()) {
            throw new RuntimeException("Cannot delete album that still contains songs. Remove all songs first.");
        }

        albumRepository.delete(album);
    }

    /**
     * Add a song to an album (artist only, own songs).
     */
    @Transactional
    public AlbumDTO addSongToAlbum(Long albumId, Long songId, User artist) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        if (!album.getArtist().getId().equals(artist.getId())) {
            throw new RuntimeException("You can only add songs to your own albums");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        if (!song.getArtist().getId().equals(artist.getId())) {
            throw new RuntimeException("You can only add your own songs to your album");
        }

        song.setAlbum(album);
        songRepository.save(song);
        return toDTO(album, true);
    }

    /**
     * Remove a song from an album.
     */
    @Transactional
    public AlbumDTO removeSongFromAlbum(Long albumId, Long songId, User artist) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        if (!album.getArtist().getId().equals(artist.getId())) {
            throw new RuntimeException("You can only manage your own albums");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        if (song.getAlbum() != null && song.getAlbum().getId().equals(albumId)) {
            song.setAlbum(null);
            songRepository.save(song);
        }

        return toDTO(album, true);
    }

    /**
     * Convert Album entity to AlbumDTO.
     */
    private AlbumDTO toDTO(Album album, boolean includeSongs) {
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getId());
        dto.setName(album.getName());
        dto.setDescription(album.getDescription());
        dto.setCoverArt(album.getCoverArtData() != null ? "has_cover" : null);
        dto.setReleaseDate(album.getReleaseDate() != null ? album.getReleaseDate().toString() : null);
        dto.setArtistId(album.getArtist().getId());
        dto.setArtistName(album.getArtist().getUsername());

        List<Song> songs = songRepository.findByAlbumId(album.getId());
        dto.setSongCount(songs.size());

        if (includeSongs) {
            List<SongDTO> songDTOs = songs.stream()
                    .map(songService::toDTO)
                    .collect(Collectors.toList());
            dto.setSongs(songDTOs);
        }

        return dto;
    }
}
