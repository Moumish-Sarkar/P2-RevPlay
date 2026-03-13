package com.revplay.service;

import com.revplay.dto.FavoriteDTO;
import com.revplay.model.Favorite;
import com.revplay.model.Song;
import com.revplay.model.User;
import com.revplay.repository.FavoriteRepository;
import com.revplay.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final SongRepository songRepository;
    private final SongService songService;

    public FavoriteService(FavoriteRepository favoriteRepository, SongRepository songRepository,
            SongService songService) {
        this.favoriteRepository = favoriteRepository;
        this.songRepository = songRepository;
        this.songService = songService;
    }

    @Transactional
    public boolean toggleFavorite(Long songId, User user) {
        Optional<Favorite> existingFav = favoriteRepository.findByUserIdAndSongId(user.getId(), songId);
        if (existingFav.isPresent()) {
            favoriteRepository.delete(existingFav.get());
            return false;
        } else {
            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new RuntimeException("Song not found"));
            Favorite favorite = new Favorite(user, song);
            favoriteRepository.save(favorite);
            return true;
        }
    }

    public boolean isFavorite(Long songId, User user) {
        return favoriteRepository.existsByUserIdAndSongId(user.getId(), songId);
    }

    public List<FavoriteDTO> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private FavoriteDTO toDTO(Favorite favorite) {
        FavoriteDTO dto = new FavoriteDTO();
        dto.setId(favorite.getId());
        dto.setSongId(favorite.getSong().getId());
        dto.setCreatedAt(favorite.getCreatedAt() != null ? favorite.getCreatedAt().toString() : null);
        dto.setSong(songService.toDTO(favorite.getSong()));
        return dto;
    }
}
