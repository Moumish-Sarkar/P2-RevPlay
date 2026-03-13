package com.revplay.service;

import com.revplay.dto.PlaylistDTO;
import com.revplay.dto.SongDTO;
import com.revplay.model.Playlist;
import com.revplay.model.PlaylistSong;
import com.revplay.model.Song;
import com.revplay.model.User;
import com.revplay.repository.PlaylistRepository;
import com.revplay.repository.PlaylistSongRepository;
import com.revplay.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    public PlaylistService(PlaylistRepository playlistRepository, PlaylistSongRepository playlistSongRepository,
            SongRepository songRepository, SongService songService) {
        this.playlistRepository = playlistRepository;
        this.playlistSongRepository = playlistSongRepository;
        this.songRepository = songRepository;
        this.songService = songService;
    }

    public List<PlaylistDTO> getUserPlaylists(Long userId) {
        return playlistRepository.findByOwnerId(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PlaylistDTO createPlaylist(String name, String description, Boolean isPublic, User owner) {
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setIsPublic(isPublic != null ? isPublic : true);
        playlist.setOwner(owner);
        Playlist saved = playlistRepository.save(playlist);
        return toDTO(saved);
    }

    public PlaylistDTO getPlaylistById(Long id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        return toDTO(playlist);
    }

    @Transactional
    public void deletePlaylist(Long id, User user) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        if (!playlist.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this playlist");
        }
        playlistRepository.deleteById(id);
    }

    @Transactional
    public void addSongToPlaylist(Long playlistId, Long songId, User user) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to modify this playlist");
        }

        Optional<PlaylistSong> existing = playlistSongRepository.findByPlaylistIdAndSongId(playlistId, songId);
        if (existing.isPresent()) {
            throw new RuntimeException("Song already in playlist");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        Integer maxPosition = playlistSongRepository.findMaxPositionByPlaylistId(playlistId);
        int newPosition = maxPosition != null ? maxPosition + 1 : 1;

        PlaylistSong playlistSong = new PlaylistSong(playlist, song, newPosition);
        playlistSongRepository.save(playlistSong);
    }

    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId, User user) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to modify this playlist");
        }

        playlistSongRepository.deleteByPlaylistIdAndSongId(playlistId, songId);
    }

    private PlaylistDTO toDTO(Playlist playlist) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        dto.setDescription(playlist.getDescription());
        dto.setIsPublic(playlist.getIsPublic());
        if (playlist.getOwner() != null) {
            dto.setOwnerId(playlist.getOwner().getId());
            dto.setOwnerName(playlist.getOwner().getDisplayName() != null ? playlist.getOwner().getDisplayName()
                    : playlist.getOwner().getUsername());
        }
        dto.setCreatedAt(playlist.getCreatedAt() != null ? playlist.getCreatedAt().toString() : null);

        if (playlist.getSongs() != null) {
            List<SongDTO> songDTOs = playlist.getSongs().stream()
                    .map(ps -> songService.toDTO(ps.getSong()))
                    .collect(Collectors.toList());
            dto.setSongs(songDTOs);
        }

        return dto;
    }
}
