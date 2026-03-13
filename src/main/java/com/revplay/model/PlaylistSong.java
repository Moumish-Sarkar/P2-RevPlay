package com.revplay.model;

import jakarta.persistence.*;

@Entity
@Table(name = "playlist_songs")
public class PlaylistSong {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ps_seq")
    @SequenceGenerator(name = "ps_seq", sequenceName = "PLAYLIST_SONG_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Column(nullable = false)
    private Integer position;

    // Constructors
    public PlaylistSong() {}

    public PlaylistSong(Playlist playlist, Song song, Integer position) {
        this.playlist = playlist;
        this.song = song;
        this.position = position;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Playlist getPlaylist() { return playlist; }
    public void setPlaylist(Playlist playlist) { this.playlist = playlist; }

    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
