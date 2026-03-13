package com.revplay.model;

import jakarta.persistence.*;

@Entity
@Table(name = "social_links")
public class SocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "social_link_seq")
    @SequenceGenerator(name = "social_link_seq", sequenceName = "SOCIAL_LINK_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Column(nullable = false)
    private String platform; // INSTAGRAM, TWITTER, YOUTUBE, SPOTIFY, WEBSITE

    @Column(nullable = false)
    private String url;

    // Constructors
    public SocialLink() {}

    public SocialLink(ArtistProfile artistProfile, String platform, String url) {
        this.artistProfile = artistProfile;
        this.platform = platform;
        this.url = url;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ArtistProfile getArtistProfile() { return artistProfile; }
    public void setArtistProfile(ArtistProfile artistProfile) { this.artistProfile = artistProfile; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
