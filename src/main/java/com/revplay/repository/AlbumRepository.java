package com.revplay.repository;

import com.revplay.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByArtistId(Long artistId);

    List<Album> findAllByOrderByCreatedAtDesc();

    long countByArtistId(Long artistId);
}
