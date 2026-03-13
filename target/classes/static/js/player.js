/**
 * RevPlay Audio Player
 * Manages HTML5 audio playback, progress, volume, and queue.
 */
const Player = (() => {
    let audio = null;
    let currentSong = null;
    let queue = [];
    let queueIndex = -1;
    let isPlaying = false;
    let isMuted = false;
    let savedVolume = 0.7;

    // ---- DOM References ----
    function el(id) { return document.getElementById(id); }

    // ---- Initialize ----
    function init() {
        audio = el('audioElement');
        if (!audio) return;

        audio.volume = savedVolume;

        // Audio events
        audio.addEventListener('timeupdate', onTimeUpdate);
        audio.addEventListener('loadedmetadata', onLoadedMetadata);
        audio.addEventListener('ended', onEnded);
        audio.addEventListener('play', () => updatePlayButton(true));
        audio.addEventListener('pause', () => updatePlayButton(false));
        audio.addEventListener('error', onError);
    }

    // ---- Play a Song ----
    function play(song) {
        if (!audio || !song) return;

        currentSong = song;
        audio.src = API.getStreamUrl(song.id);
        audio.load();
        audio.play().catch(() => { });
        isPlaying = true;

        updateUI(song);
        showPlayerBar();
        highlightCurrentCard(song.id);
    }

    // ---- Set Queue ----
    function setQueue(songs, startIndex) {
        queue = songs || [];
        queueIndex = startIndex >= 0 ? startIndex : 0;
        if (queue.length > 0) {
            play(queue[queueIndex]);
        }
    }

    // ---- Play Next ----
    function next() {
        if (queue.length === 0) return;
        queueIndex = (queueIndex + 1) % queue.length;
        play(queue[queueIndex]);
    }

    // ---- Play Previous ----
    function prev() {
        if (queue.length === 0) return;
        // If more than 3 seconds into the song, restart it instead
        if (audio && audio.currentTime > 3) {
            audio.currentTime = 0;
            return;
        }
        queueIndex = (queueIndex - 1 + queue.length) % queue.length;
        play(queue[queueIndex]);
    }

    // ---- Toggle Play/Pause ----
    function togglePlay() {
        if (!audio || !currentSong) return;

        if (audio.paused) {
            audio.play().catch(() => { });
            isPlaying = true;
        } else {
            audio.pause();
            isPlaying = false;
        }
    }

    // ---- Seek ----
    function seek(event) {
        if (!audio || !audio.duration) return;
        const bar = event.currentTarget;
        const rect = bar.getBoundingClientRect();
        const x = event.clientX - rect.left;
        const percent = x / rect.width;
        audio.currentTime = percent * audio.duration;
    }

    // ---- Volume ----
    function setVol(event) {
        if (!audio) return;
        const bar = event.currentTarget;
        const rect = bar.getBoundingClientRect();
        const x = event.clientX - rect.left;
        const percent = Math.max(0, Math.min(1, x / rect.width));
        audio.volume = percent;
        savedVolume = percent;
        isMuted = percent === 0;
        updateVolumeUI();
    }

    function toggleMuteState() {
        if (!audio) return;
        if (isMuted) {
            audio.volume = savedVolume || 0.7;
            isMuted = false;
        } else {
            savedVolume = audio.volume;
            audio.volume = 0;
            isMuted = true;
        }
        updateVolumeUI();
    }

    // ---- Event Handlers ----
    function onTimeUpdate() {
        if (!audio || !audio.duration) return;

        const currentTimeEl = el('currentTime');
        const progressFill = el('progressFill');

        if (currentTimeEl) currentTimeEl.textContent = formatTime(audio.currentTime);
        if (progressFill) progressFill.style.width = (audio.currentTime / audio.duration * 100) + '%';
    }

    function onLoadedMetadata() {
        const totalTimeEl = el('totalTime');
        if (totalTimeEl && audio.duration && isFinite(audio.duration)) {
            totalTimeEl.textContent = formatTime(audio.duration);
        }
    }

    function onEnded() {
        isPlaying = false;
        next();
    }

    function onError() {
        console.error('Audio playback error');
        if (typeof showToast === 'function') {
            showToast('Failed to play this song', 'error');
        }
    }

    function updateUI(song) {
        const titleEl = el('playerTitle');
        const artistEl = el('playerArtist');
        const coverEl = el('playerCover');

        if (titleEl) titleEl.textContent = song.title || 'Unknown';
        if (artistEl) artistEl.textContent = song.artistName || 'Unknown Artist';

        if (coverEl) {
            if (song.coverArtUrl) {
                coverEl.innerHTML = '<img src="' + API.getCoverUrl(song.id) + '" alt="Cover art">';
            } else {
                coverEl.innerHTML = '<div class="cover-placeholder-mini">&#127925;</div>';
            }
        }

        updateRightPanelUI(song);
    }

    function updateRightPanelUI(song) {
        const rightPanel = el('rightPanel');
        if (!rightPanel) return;

        // Show panel if hidden
        rightPanel.style.display = 'flex';

        // Update Text
        const titleEl = el('rightPanelTitle');
        const artistEl = el('rightPanelArtist');
        const aboutArtistEl = el('rightPanelAboutArtist');
        const avatarEl = el('rightPanelArtistAvatar');

        const artistName = song.artistName || 'Unknown Artist';
        if (titleEl) titleEl.textContent = song.title || 'Unknown';
        if (artistEl) artistEl.textContent = artistName;
        if (aboutArtistEl) aboutArtistEl.textContent = artistName;

        if (avatarEl) {
            avatarEl.textContent = artistName.charAt(0).toUpperCase();
        }

        // Update Cover
        const coverEl = el('rightPanelCover');
        if (coverEl) {
            if (song.coverArtUrl) {
                coverEl.innerHTML = '<img src="' + API.getCoverUrl(song.id) + '" alt="Cover art">';
            } else {
                coverEl.innerHTML = '<div class="cover-placeholder" style="width:100%; height:100%; font-size: 3rem;">&#127925;</div>';
            }
        }
    }

    function updatePlayButton(playing) {
        const btn = el('playPauseBtn');
        if (btn) {
            btn.innerHTML = playing ? '&#10074;&#10074;' : '&#9654;';
            btn.title = playing ? 'Pause' : 'Play';
        }
    }

    function updateVolumeUI() {
        const volumeFill = el('volumeFill');
        const volumeBtn = el('volumeBtn');

        if (volumeFill && audio) {
            volumeFill.style.width = (audio.volume * 100) + '%';
        }
        if (volumeBtn) {
            if (isMuted || (audio && audio.volume === 0)) {
                volumeBtn.innerHTML = '&#128263;';
            } else if (audio && audio.volume < 0.5) {
                volumeBtn.innerHTML = '&#128265;';
            } else {
                volumeBtn.innerHTML = '&#128266;';
            }
        }
    }

    function showPlayerBar() {
        const bar = el('playerBar');
        if (bar) bar.classList.add('show');
    }

    function highlightCurrentCard(songId) {
        // Remove previous highlight
        document.querySelectorAll('.song-card.playing').forEach(card => {
            card.classList.remove('playing');
        });
        // Add highlight to current
        const card = document.querySelector('[data-song-id="' + songId + '"]');
        if (card) card.classList.add('playing');
    }

    // ---- Utility ----
    function formatTime(seconds) {
        if (!seconds || !isFinite(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return mins + ':' + (secs < 10 ? '0' : '') + secs;
    }

    function getCurrentSong() {
        return currentSong;
    }

    function getIsPlaying() {
        return isPlaying;
    }

    // ---- Public API ----
    return {
        init,
        play,
        setQueue,
        next,
        prev,
        togglePlay,
        seek,
        setVol,
        toggleMuteState,
        getCurrentSong,
        getIsPlaying
    };
})();
