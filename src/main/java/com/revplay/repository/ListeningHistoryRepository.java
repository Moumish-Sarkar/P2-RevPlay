package com.revplay.repository;

import com.revplay.model.ListeningHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ListeningHistoryRepository extends JpaRepository<ListeningHistory, Long> {

    List<ListeningHistory> findByUserIdOrderByPlayedAtDesc(Long userId);

    @Query("SELECT lh FROM ListeningHistory lh WHERE lh.user.id = :userId ORDER BY lh.playedAt DESC")
    List<ListeningHistory> findRecentByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);

    @Query("SELECT lh.song.id, COUNT(lh) as cnt FROM ListeningHistory lh WHERE lh.song.artist.id = :artistId GROUP BY lh.song.id ORDER BY cnt DESC")
    List<Object[]> findPlayCountsByArtist(@Param("artistId") Long artistId);

    @Query("SELECT lh.user.id, lh.user.username, COUNT(lh) as cnt FROM ListeningHistory lh WHERE lh.song.artist.id = :artistId GROUP BY lh.user.id, lh.user.username ORDER BY cnt DESC")
    List<Object[]> findTopListenersByArtist(@Param("artistId") Long artistId);

    @Query("SELECT COUNT(lh) FROM ListeningHistory lh WHERE lh.song.artist.id = :artistId")
    long countTotalPlaysByArtist(@Param("artistId") Long artistId);

    @Query("SELECT COUNT(lh) FROM ListeningHistory lh WHERE lh.song.artist.id = :artistId AND lh.playedAt >= :since")
    long countPlaysByArtistSince(@Param("artistId") Long artistId, @Param("since") LocalDateTime since);
}
