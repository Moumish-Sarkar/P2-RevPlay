package com.revplay.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Represents a playable audio track in the system.
 * This entity stores actual media bytes (@Lob) for the song and cover art
 * directly in the database.
 * Every song is tied to an ARTIST (User) and optionally belongs to an Album.
 */
@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "song_seq")
    @SequenceGenerator(name = "song_seq", sequenceName = "SONG_SEQ", allocationSize = 1)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    private String genre;

    private Integer duration; // in seconds

    @Lob
    @Column(name = "audio_data")
    private byte[] audioData;

    @Column(name = "audio_content_type")
    private String audioContentType;

    @Lob
    @Column(name = "cover_art_data")
    private byte[] coverArtData;

    @Column(name = "cover_art_content_type")
    private String coverArtContentType;

    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "play_count")
    private Long playCount = 0L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "artist_id", nullable = false)
    private User artist;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "album_id")
    private Album album;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Visibility {
        PUBLIC, UNLISTED
    }

    // Constructors
    public Song() {
    }

    /**
     * Lightweight constructor for projections that exclude BLOB data.
     * Used by repository queries to avoid loading audio/cover bytes.
     */
    public Song(Long id, String title, String genre, Integer duration,
            String audioContentType, String coverArtContentType,
            Visibility visibility, Long playCount,
            User artist, Album album, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.audioContentType = audioContentType;
        this.coverArtContentType = coverArtContentType;
        this.visibility = visibility;
        this.playCount = playCount;
        this.artist = artist;
        this.album = album;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }

    public String getAudioContentType() {
        return audioContentType;
    }

    public void setAudioContentType(String audioContentType) {
        this.audioContentType = audioContentType;
    }

    public byte[] getCoverArtData() {
        return coverArtData;
    }

    public void setCoverArtData(byte[] coverArtData) {
        this.coverArtData = coverArtData;
    }

    public String getCoverArtContentType() {
        return coverArtContentType;
    }

    public void setCoverArtContentType(String coverArtContentType) {
        this.coverArtContentType = coverArtContentType;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long playCount) {
        this.playCount = playCount;
    }

    public User getArtist() {
        return artist;
    }

    public void setArtist(User artist) {
        this.artist = artist;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
