package com.revplay.repository;

import com.revplay.model.Song;
import com.revplay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Song entities.
 * Handles all database operations related to songs, including complex searches
 * and performance-optimized queries that avoid loading heavy BLOB data.
 */
@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

        List<Song> findByArtist(User artist);

        List<Song> findByArtistId(Long artistId);

        List<Song> findByAlbumId(Long albumId);

        /**
         * Lightweight query — selects all fields EXCEPT the BLOB columns (audioData,
         * coverArtData).
         * Uses the projection constructor in Song.java.
         */
        @Query("SELECT new com.revplay.model.Song(s.id, s.title, s.genre, s.duration, " +
                        "s.audioContentType, s.coverArtContentType, s.visibility, s.playCount, " +
                        "a, al, s.createdAt) " +
                        "FROM Song s JOIN s.artist a LEFT JOIN s.album al WHERE s.visibility = 'PUBLIC'")
        List<Song> findAllPublicWithoutBlob();

        /**
         * Search songs by a keyword (checks title, genre, and artist's display name).
         * This uses JPQL (Java Persistence Query Language) to perform advanced
         * filtering
         * while also utilizing the lightweight projection constructor to avoid loading
         * audio blobs.
         */
        @Query("SELECT new com.revplay.model.Song(s.id, s.title, s.genre, s.duration, " +
                        "s.audioContentType, s.coverArtContentType, s.visibility, s.playCount, " +
                        "a, al, s.createdAt) " +
                        "FROM Song s JOIN s.artist a LEFT JOIN s.album al WHERE s.visibility = 'PUBLIC' AND " +
                        "(LOWER(s.title) LIKE LOWER(CONCAT('%',:query,'%')) OR " +
                        "LOWER(s.genre) LIKE LOWER(CONCAT('%',:query,'%')) OR " +
                        "LOWER(a.displayName) LIKE LOWER(CONCAT('%',:query,'%')))")
        List<Song> searchSongsWithoutBlob(@Param("query") String query);

        @Query("SELECT s FROM Song s JOIN s.artist a WHERE s.visibility = 'PUBLIC' AND " +
                        "(LOWER(s.title) LIKE LOWER(CONCAT('%',:query,'%')) OR " +
                        "LOWER(s.genre) LIKE LOWER(CONCAT('%',:query,'%')) OR " +
                        "LOWER(a.displayName) LIKE LOWER(CONCAT('%',:query,'%')))")
        List<Song> searchSongs(@Param("query") String query);

        @Query("SELECT s FROM Song s WHERE s.visibility = 'PUBLIC' AND " +
                        "(:genre IS NULL OR s.genre = :genre) AND " +
                        "(:artistId IS NULL OR s.artist.id = :artistId)")
        List<Song> filterSongs(@Param("genre") String genre, @Param("artistId") Long artistId);

        List<Song> findByVisibilityOrderByPlayCountDesc(Song.Visibility visibility);

        @Query("SELECT DISTINCT s.genre FROM Song s WHERE s.genre IS NOT NULL")
        List<String> findAllGenres();

        List<Song> findByVisibility(Song.Visibility visibility);

        @Query("SELECT new com.revplay.model.Song(s.id, s.title, s.genre, s.duration, " +
                        "s.audioContentType, s.coverArtContentType, s.visibility, s.playCount, " +
                        "a, al, s.createdAt) " +
                        "FROM Song s JOIN s.artist a LEFT JOIN s.album al WHERE s.visibility = 'PUBLIC' ORDER BY s.playCount DESC")
        List<Song> findTopSongsWithoutBlob();

        @Query("SELECT s FROM Song s WHERE s.visibility = 'PUBLIC' ORDER BY s.playCount DESC")
        List<Song> findTopSongs();

        long countByArtistId(Long artistId);

        @Query("SELECT new com.revplay.model.Song(s.id, s.title, s.genre, s.duration, " +
                        "s.audioContentType, s.coverArtContentType, s.visibility, s.playCount, " +
                        "a, al, s.createdAt) " +
                        "FROM Song s JOIN s.artist a LEFT JOIN s.album al WHERE a.id = :artistId")
        List<Song> findByArtistIdWithoutBlob(@Param("artistId") Long artistId);
}
