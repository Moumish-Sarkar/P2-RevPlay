package com.revplay.repository;

import com.revplay.model.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    List<SocialLink> findByArtistProfileId(Long artistProfileId);
    void deleteByArtistProfileIdAndId(Long artistProfileId, Long id);
}
