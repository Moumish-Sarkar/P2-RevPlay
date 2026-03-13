package com.revplay.repository;

import com.revplay.model.Playlist;
import com.revplay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByOwner(User owner);
    List<Playlist> findByOwnerId(Long ownerId);
    List<Playlist> findByIsPublicTrue();

    @Query("SELECT p FROM Playlist p JOIN p.followers f WHERE f.id = :userId")
    List<Playlist> findFollowedPlaylists(@Param("userId") Long userId);

    long countByOwnerId(Long ownerId);
}
