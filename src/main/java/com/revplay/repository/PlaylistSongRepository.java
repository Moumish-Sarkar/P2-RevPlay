package com.revplay.repository;

import com.revplay.model.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {
    Optional<PlaylistSong> findByPlaylistIdAndSongId(Long playlistId, Long songId);

    @Query("SELECT MAX(ps.position) FROM PlaylistSong ps WHERE ps.playlist.id = :playlistId")
    Integer findMaxPositionByPlaylistId(@Param("playlistId") Long playlistId);

    void deleteByPlaylistIdAndSongId(Long playlistId, Long songId);
}
