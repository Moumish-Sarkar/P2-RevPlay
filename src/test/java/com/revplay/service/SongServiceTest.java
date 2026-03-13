package com.revplay.service;

import com.revplay.dto.SongDTO;
import com.revplay.model.Song;
import com.revplay.repository.AlbumRepository;
import com.revplay.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private AlbumRepository albumRepository;

    @InjectMocks
    private SongService songService;

    private Song testSong;

    @BeforeEach
    void setUp() {
        testSong = new Song();
        testSong.setId(1L);
        testSong.setTitle("Mock Song");
        testSong.setGenre("Pop");
        testSong.setDuration(180);
        testSong.setPlayCount(0L);
    }

    @Test
    void testGetAllPublicSongs() {
        // Arrange
        when(songRepository.findAllPublicWithoutBlob()).thenReturn(Arrays.asList(testSong));

        // Act
        List<SongDTO> results = songService.getAllPublicSongs();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Mock Song", results.get(0).getTitle());
        assertEquals("Pop", results.get(0).getGenre());
        verify(songRepository, times(1)).findAllPublicWithoutBlob();
    }

    @Test
    void testGetSongByIdSuccess() {
        // Arrange
        when(songRepository.findById(1L)).thenReturn(Optional.of(testSong));

        // Act
        SongDTO result = songService.getSongById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Mock Song", result.getTitle());
    }

    @Test
    void testGetSongByIdNotFound() {
        // Arrange
        when(songRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            songService.getSongById(99L);
        });

        assertTrue(exception.getMessage().contains("Song not found"));
    }

    @Test
    void testIncrementPlayCount() {
        // Arrange
        when(songRepository.findById(1L)).thenReturn(Optional.of(testSong));
        when(songRepository.save(any(Song.class))).thenReturn(testSong);

        // Act
        songService.incrementPlayCount(1L);

        // Assert
        assertEquals(1L, testSong.getPlayCount());
        verify(songRepository, times(1)).save(testSong);
    }
}
