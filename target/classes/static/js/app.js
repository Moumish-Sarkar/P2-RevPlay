/**
 * RevPlay Main App Logic
 * Handles the index page — songs grid, search, genres, upload, user menu.
 */

// ---- State ----
let allSongs = [];
let topSongs = [];
let currentGenre = 'all';
let searchDebounceTimer = null;
window.userFavorites = new Set();
window.userPlaylists = [];

document.addEventListener('DOMContentLoaded', () => {
    Player.init();
    setupAuthUI();
    loadGenres();
    loadTopSongs();
    loadAllSongs();
    loadAlbumsHome();
    setupSearch();
    setupClickOutside();
    if (API.isLoggedIn()) {
        initUserLibrary();
    }
});

async function initUserLibrary() {
    try {
        const favRes = await API.getMyFavorites();
        if (favRes.success && favRes.data) {
            window.userFavorites = new Set(favRes.data.map(f => f.songId));
        }
        const plRes = await API.getMyPlaylists();
        if (plRes.success && plRes.data) {
            window.userPlaylists = plRes.data;
        }
        // Re-render grids if they're already populated
        if (allSongs.length > 0) renderSongGrid('allSongsGrid', allSongs);
        if (topSongs.length > 0) renderSongGrid('topSongsGrid', topSongs.slice(0, 8));
    } catch (e) {
        console.error('Failed to init user library:', e);
    }
}

// ---- Auth UI Setup ----
function setupAuthUI() {
    const user = API.getUser();
    const loginLink = document.getElementById('loginLink');
    const userMenu = document.getElementById('userMenu');
    const navMenu = document.getElementById('navMenu');
    const uploadBtn = document.getElementById('uploadBtn');
    const userAvatar = document.getElementById('userAvatar');
    const dropdownName = document.getElementById('dropdownName');
    const dropdownRole = document.getElementById('dropdownRole');

    if (API.isLoggedIn() && user) {
        if (loginLink) loginLink.style.display = 'none';
        if (userMenu) userMenu.style.display = 'flex';
        if (navMenu) navMenu.style.display = 'flex';

        // Avatar initial
        const initial = (user.displayName || user.username || 'U').charAt(0).toUpperCase();
        if (userAvatar) userAvatar.textContent = initial;

        // Dropdown info
        if (dropdownName) dropdownName.textContent = user.displayName || user.username;
        if (dropdownRole) dropdownRole.textContent = user.role === 'ARTIST' ? 'Artist' : 'Listener';

        // Show upload button and albums tab for artists
        if (user.role === 'ARTIST' && uploadBtn) {
            uploadBtn.style.display = 'inline-flex';
        }
        const tabAlbums = document.getElementById('tabAlbums');
        if (user.role === 'ARTIST' && tabAlbums) {
            tabAlbums.style.display = 'block';
        }

        // Personalize hero
        const heroTitle = document.querySelector('.hero-title');
        if (heroTitle) {
            const name = user.displayName || user.username;
            heroTitle.innerHTML = 'Welcome back, <span>' + escapeHtml(name) + '</span>';
        }
    } else {
        if (loginLink) loginLink.style.display = 'inline-flex';
        if (userMenu) userMenu.style.display = 'none';
        if (navMenu) navMenu.style.display = 'none';
    }
}

// ---- User Menu Toggle ----
function toggleUserDropdown(event) {
    if (event) event.stopPropagation();
    const dropdown = document.getElementById('userDropdownMenu');
    if (dropdown) {
        dropdown.classList.toggle('show');
    }
}

// ---- Click Outside ----
function setupClickOutside() {
    document.addEventListener('click', (event) => {
        const userMenu = document.getElementById('userMenu');
        const dropdown = document.getElementById('userDropdownMenu');

        // If clicking outside the user menu, close the dropdown
        if (userMenu && dropdown && !userMenu.contains(event.target)) {
            dropdown.classList.remove('show');
        }
    });
}

// ---- Logout ----
function logout() {
    API.logout();
    window.location.href = 'login.html';
}

// ---- Load Genres ----
async function loadGenres() {
    try {
        const res = await API.getGenres();
        if (res.success && res.data) {
            renderGenres(res.data);
        }
    } catch (e) {
        console.error('Failed to load genres:', e);
    }
}

// ---- Right Panel ----
function closeRightPanel() {
    const panel = document.getElementById('rightPanel');
    if (panel) {
        panel.style.display = 'none';
    }
}
function renderGenres(genres) {
    const container = document.getElementById('genrePills');
    if (!container) return;

    // Keep the "All" pill
    container.innerHTML = '<button class="genre-pill active" data-genre="all" onclick="filterByGenre(\'all\')">All</button>';

    genres.forEach(genre => {
        if (!genre) return;
        const pill = document.createElement('button');
        pill.className = 'genre-pill';
        pill.dataset.genre = genre;
        pill.textContent = genre;
        pill.onclick = () => filterByGenre(genre);
        container.appendChild(pill);
    });
}

// ---- Filter by Genre ----
function filterByGenre(genre) {
    currentGenre = genre;

    // Update active pill
    document.querySelectorAll('.genre-pill').forEach(pill => {
        pill.classList.toggle('active', pill.dataset.genre === genre);
    });

    const topTitle = document.getElementById('topSongsTitle');
    const allTitle = document.getElementById('allSongsTitle');

    // Filter songs
    if (genre === 'all') {
        if (topTitle) topTitle.innerHTML = '&#128293; Trending Now';
        if (allTitle) allTitle.innerHTML = '&#127925; All Songs';

        renderSongGrid('allSongsGrid', allSongs);
        renderSongGrid('topSongsGrid', topSongs.slice(0, 8));
    } else {
        if (topTitle) topTitle.innerHTML = '&#128293; Trending in ' + genre;
        if (allTitle) allTitle.innerHTML = '&#127925; All Songs';

        renderSongGrid('allSongsGrid', allSongs);

        const filteredTop = topSongs.filter(s =>
            s.genre && s.genre.toLowerCase() === genre.toLowerCase()
        );
        renderSongGrid('topSongsGrid', filteredTop.slice(0, 8));
    }
}

// ---- Load Top Songs ----
async function loadTopSongs() {
    try {
        const res = await API.getTopSongs();
        if (res.success && res.data) {
            topSongs = res.data;
            renderSongGrid('topSongsGrid', topSongs.slice(0, 8));

            if (topSongs.length === 0) {
                showEmptyState('topSongsGrid', 'No trending songs yet', 'Be the first to upload a track!');
            }
        }
    } catch (e) {
        console.error('Failed to load top songs:', e);
        showEmptyState('topSongsGrid', 'Could not load songs', 'Please try again later');
    }
}

// ---- Load All Songs ----
async function loadAllSongs() {
    try {
        const res = await API.getSongs();
        if (res.success && res.data) {
            allSongs = res.data;
            renderSongGrid('allSongsGrid', allSongs);

            if (allSongs.length === 0) {
                showEmptyState('allSongsGrid', 'No songs yet', 'Upload your first track to get started!');
            }
        }
    } catch (e) {
        console.error('Failed to load songs:', e);
        showEmptyState('allSongsGrid', 'Could not load songs', 'Please try again later');
    }
}

// ---- Render Song Grid ----
function renderSongGrid(containerId, songs) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!songs || songs.length === 0) {
        showEmptyState(containerId, 'No songs found', 'Try a different search or genre');
        return;
    }

    container.innerHTML = songs.map((song, index) => {
        const coverHtml = song.coverArtUrl
            ? '<img src="' + API.getCoverUrl(song.id) + '" alt="' + escapeHtml(song.title) + '" loading="lazy">'
            : '<div class="cover-placeholder">&#127925;</div>';

        const playCount = song.playCount ? formatNumber(song.playCount) : '0';
        const duration = song.duration ? formatDuration(song.duration) : '';

        let actionsHtml = '';
        if (API.isLoggedIn()) {
            const isFav = window.userFavorites && window.userFavorites.has(song.id);
            const heartIcon = isFav ? '&#10084;&#65039;' : '&#9825;';
            actionsHtml = `
                <div class="song-card-actions">
                    <button class="btn-favorite ${isFav ? 'active' : ''}" onclick="event.stopPropagation(); toggleFavorite(${song.id}, this)" title="Favorite">${heartIcon}</button>
                </div>
            `;
        }

        return `
            <div class="song-card" data-song-id="${song.id}" onclick="playSongFromGrid('${containerId}', ${index})">
                <div class="song-card-cover">
                    ${coverHtml}
                    <button class="song-card-play" onclick="event.stopPropagation(); playSongFromGrid('${containerId}', ${index})">&#9654;</button>
                </div>
                ${actionsHtml}
                <div class="song-card-title" title="${escapeHtml(song.title)}">${escapeHtml(song.title)}</div>
                <div class="song-card-artist">${escapeHtml(song.artistName || 'Unknown')}</div>
                <div class="song-card-meta">
                    <span>&#9654; ${playCount}</span>
                    ${duration ? '<span>· ' + duration + '</span>' : ''}
                    ${song.genre ? '<span>· ' + escapeHtml(song.genre) + '</span>' : ''}
                </div>
            </div>
        `;
    }).join('');
}

// ---- Play Song from Grid ----
function playSongFromGrid(containerId, index) {
    let songs;
    if (containerId === 'topSongsGrid') {
        songs = currentGenre === 'all' ? topSongs.slice(0, 8) : topSongs.filter(s =>
            s.genre && s.genre.toLowerCase() === currentGenre.toLowerCase()
        ).slice(0, 8);
    } else if (containerId === 'searchResultsGrid') {
        songs = window._searchResults || [];
    } else if (containerId === 'favoritesGrid') {
        songs = window._libraryFavorites || [];
    } else if (containerId === 'playlistSongsGrid') {
        songs = window._playlistSongs || [];
    } else if (containerId === 'albumSongsGrid') {
        songs = (window._currentAlbum && window._currentAlbum.songs) ? window._currentAlbum.songs : [];
    } else {
        // "All Songs" grid is always unfiltered now
        songs = allSongs;
    }
    Player.setQueue(songs, index);
}

// ---- Search ----
function setupSearch() {
    const searchInput = document.getElementById('searchInput');
    if (!searchInput) return;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.trim();
        clearTimeout(searchDebounceTimer);

        if (!query) {
            clearSearch();
            return;
        }

        searchDebounceTimer = setTimeout(() => {
            performSearch(query);
        }, 400);
    });
}

async function performSearch(query) {
    try {
        const res = await API.searchSongs(query);
        if (res.success && res.data) {
            window._searchResults = res.data;

            // Show search results, hide other sections
            document.getElementById('heroSection').style.display = 'none';
            document.getElementById('genreSection').style.display = 'none';
            document.getElementById('topSongsSection').style.display = 'none';
            document.getElementById('allSongsSection').style.display = 'none';
            document.getElementById('searchResultsSection').style.display = 'block';
            document.getElementById('albumsSection').style.display = 'block';

            if (window._allAlbums) {
                const queryLower = query.toLowerCase();
                const filteredAlbums = window._allAlbums.filter(a =>
                    (a.name && a.name.toLowerCase().includes(queryLower)) ||
                    (a.artistName && a.artistName.toLowerCase().includes(queryLower))
                );
                renderAlbumGrid('albumsGrid', filteredAlbums);
            }

            document.getElementById('searchQuery').textContent = `"${query}" — ${res.data.length} result${res.data.length !== 1 ? 's' : ''}`;

            renderSongGrid('searchResultsGrid', res.data);
        }
    } catch (e) {
        console.error('Search failed:', e);
    }
}

function clearSearch() {
    document.getElementById('searchInput').value = '';
    document.getElementById('heroSection').style.display = 'block';
    document.getElementById('genreSection').style.display = 'block';
    document.getElementById('topSongsSection').style.display = 'block';
    document.getElementById('allSongsSection').style.display = 'block';
    document.getElementById('albumsSection').style.display = 'block';
    document.getElementById('searchResultsSection').style.display = 'none';
    window._searchResults = [];
    if (window._allAlbums) {
        renderAlbumGrid('albumsGrid', window._allAlbums);
    }
}

// ---- Upload Modal ----
function openUploadModal(album) {
    const modal = document.getElementById('uploadModal');
    if (modal) modal.classList.add('show');

    // If uploading to an album, pre-set the album ID
    const albumIdField = document.getElementById('uploadAlbumId');
    const albumInfoEl = document.getElementById('uploadAlbumInfo');
    const albumNameEl = document.getElementById('uploadAlbumName');
    if (album && album.id) {
        albumIdField.value = album.id;
        albumNameEl.textContent = album.name;
        albumInfoEl.style.display = 'block';
        document.querySelector('#uploadModal .modal-title').textContent = 'Upload Song to Album';
    } else {
        albumIdField.value = '';
        albumInfoEl.style.display = 'none';
        document.querySelector('#uploadModal .modal-title').textContent = 'Upload a Song';
    }
}

function closeUploadModal() {
    const modal = document.getElementById('uploadModal');
    if (modal) modal.classList.remove('show');
    // Reset form
    const form = document.getElementById('uploadForm');
    if (form) form.reset();
    document.getElementById('audioFileName').textContent = '';
    document.getElementById('coverFileName').textContent = '';
    document.getElementById('uploadAlbumId').value = '';
    document.getElementById('uploadAlbumInfo').style.display = 'none';
    const errorEl = document.getElementById('uploadError');
    if (errorEl) { errorEl.classList.remove('show'); errorEl.textContent = ''; }
}

function handleFileSelect(input, displayId) {
    const display = document.getElementById(displayId);
    if (display && input.files.length > 0) {
        display.textContent = '✓ ' + input.files[0].name;
    }
}

async function handleUpload(event) {
    event.preventDefault();
    const btn = document.getElementById('uploadSubmitBtn');
    const errorEl = document.getElementById('uploadError');

    const audioFile = document.getElementById('audioFileInput').files[0];
    const coverFile = document.getElementById('coverFileInput').files[0];
    const title = document.getElementById('songTitle').value.trim();
    const genre = document.getElementById('songGenre').value.trim();
    const visibility = document.getElementById('songVisibility').value;

    if (!audioFile) {
        errorEl.textContent = 'Please select an audio file';
        errorEl.classList.add('show');
        return;
    }

    if (!title) {
        errorEl.textContent = 'Please enter a song title';
        errorEl.classList.add('show');
        return;
    }

    btn.classList.add('loading');
    btn.disabled = true;
    errorEl.classList.remove('show');

    try {
        const formData = new FormData();
        formData.append('audioFile', audioFile);
        if (coverFile) formData.append('coverArt', coverFile);
        formData.append('title', title);
        if (genre) formData.append('genre', genre);
        formData.append('visibility', visibility);

        // Include album ID if uploading to an album
        const albumId = document.getElementById('uploadAlbumId').value;
        if (albumId) {
            formData.append('albumId', albumId);
        }

        const res = await API.uploadSong(formData);
        if (res.success) {
            showToast('Song uploaded successfully!', 'success');
            const wasAlbumUpload = !!albumId;
            const currentAlbumId = albumId;
            closeUploadModal();
            // Reload songs on home
            loadAllSongs();
            loadTopSongs();
            loadGenres();
            // If this was an album upload, refresh album details
            if (wasAlbumUpload) {
                viewAlbumDetails(Number(currentAlbumId));
            }
        } else {
            errorEl.textContent = res.message || 'Upload failed';
            errorEl.classList.add('show');
        }
    } catch (error) {
        errorEl.textContent = error.message || 'Upload failed. Please try again.';
        errorEl.classList.add('show');
    } finally {
        btn.classList.remove('loading');
        btn.disabled = false;
    }
}

// ---- Player Bridge (called from HTML onclick) ----
function togglePlayPause() { Player.togglePlay(); }
function playNext() { Player.next(); }
function playPrev() { Player.prev(); }
function seekTo(event) { Player.seek(event); }
function setVolume(event) { Player.setVol(event); }
function toggleMute() { Player.toggleMuteState(); }

// ---- Empty State ----
function showEmptyState(containerId, title, subtitle) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div class="empty-state" style="grid-column: 1 / -1;">
            <div class="empty-icon">&#127925;</div>
            <h3>${title}</h3>
            <p>${subtitle}</p>
        </div>
    `;
}

// ---- Toast Notifications ----
function showToast(message, type) {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'toast ' + (type || 'info');

    const icons = { success: '✓', error: '✕', info: 'ℹ' };
    toast.innerHTML = '<span>' + (icons[type] || 'ℹ') + '</span> ' + escapeHtml(message);

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// ---- Utilities ----
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatNumber(num) {
    if (!num) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
}

function formatDuration(seconds) {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return mins + ':' + (secs < 10 ? '0' : '') + secs;
}

// ==== Library & Favorites Logic ==== //

function showHome() {
    document.getElementById('librarySection').style.display = 'none';
    document.getElementById('playlistDetailsSection').style.display = 'none';
    document.getElementById('albumDetailsSection').style.display = 'none';

    document.getElementById('heroSection').style.display = 'block';
    document.getElementById('genreSection').style.display = 'block';
    document.getElementById('topSongsSection').style.display = 'block';
    document.getElementById('allSongsSection').style.display = 'block';
    document.getElementById('albumsSection').style.display = 'block';
    document.getElementById('searchResultsSection').style.display = 'none';

    const navHome = document.getElementById('navHomeBtn');
    const navLib = document.getElementById('navLibraryBtn');
    if (navHome) navHome.classList.add('active');
    if (navLib) navLib.classList.remove('active');
}

function showLibrary() {
    if (!API.isLoggedIn()) {
        window.location.href = 'login.html';
        return;
    }
    document.getElementById('heroSection').style.display = 'none';
    document.getElementById('genreSection').style.display = 'none';
    document.getElementById('topSongsSection').style.display = 'none';
    document.getElementById('allSongsSection').style.display = 'none';
    document.getElementById('albumsSection').style.display = 'none';
    document.getElementById('searchResultsSection').style.display = 'none';
    document.getElementById('playlistDetailsSection').style.display = 'none';
    document.getElementById('albumDetailsSection').style.display = 'none';

    document.getElementById('librarySection').style.display = 'block';
    switchLibraryTab('favorites');

    const navHome = document.getElementById('navHomeBtn');
    const navLib = document.getElementById('navLibraryBtn');
    if (navHome) navHome.classList.remove('active');
    if (navLib) navLib.classList.add('active');
}

function switchLibraryTab(tab) {
    document.getElementById('tabFavorites').classList.toggle('active', tab === 'favorites');
    document.getElementById('tabPlaylists').classList.toggle('active', tab === 'playlists');
    const tabAlbumsEl = document.getElementById('tabAlbums');
    if (tabAlbumsEl) tabAlbumsEl.classList.toggle('active', tab === 'albums');

    document.getElementById('favoritesTab').style.display = tab === 'favorites' ? 'block' : 'none';
    document.getElementById('playlistsTab').style.display = tab === 'playlists' ? 'block' : 'none';
    const albumsTabEl = document.getElementById('albumsTab');
    if (albumsTabEl) albumsTabEl.style.display = tab === 'albums' ? 'block' : 'none';

    if (tab === 'favorites') {
        loadFavorites();
    } else if (tab === 'playlists') {
        loadPlaylists();
    } else if (tab === 'albums') {
        loadMyAlbums();
    }
}

async function toggleFavorite(songId, btnEl) {
    if (!API.isLoggedIn()) {
        window.location.href = 'login.html';
        return;
    }
    try {
        const res = await API.toggleFavorite(songId);
        if (res.success) {
            const isAdded = res.data; // boolean
            if (isAdded) {
                window.userFavorites.add(songId);
                if (btnEl) {
                    btnEl.classList.add('active');
                    btnEl.innerHTML = '&#10084;&#65039;';
                }
                showToast('Added to Favorites', 'success');
            } else {
                window.userFavorites.delete(songId);
                if (btnEl) {
                    btnEl.classList.remove('active');
                    btnEl.innerHTML = '&#9825;';
                }
                showToast('Removed from Favorites', 'info');
                // If we are currently in the favorites tab, reload it
                if (document.getElementById('librarySection').style.display !== 'none' &&
                    document.getElementById('favoritesTab').style.display !== 'none') {
                    loadFavorites();
                }
            }
        }
    } catch (e) {
        showToast(e.message || 'Error toggling favorite', 'error');
    }
}

async function loadFavorites() {
    try {
        const res = await API.getMyFavorites();
        if (res.success && res.data) {
            const songs = res.data.map(f => f.song);
            window._libraryFavorites = songs;
            renderSongGrid('favoritesGrid', songs);
            if (songs.length === 0) {
                document.getElementById('favoritesGrid').innerHTML = `
                    <div class="empty-state" style="grid-column: 1 / -1; margin-top: 40px;">
                        <div class="empty-icon">&#10084;&#65039;</div>
                        <h3>No favorites yet</h3>
                        <p>Like songs to add them to your library</p>
                    </div>`;
            }
        }
    } catch (e) {
        console.error('Failed to load favorites', e);
    }
}

async function loadPlaylists() {
    try {
        const res = await API.getMyPlaylists();
        if (res.success && res.data) {
            window.userPlaylists = res.data;
            const container = document.getElementById('playlistsGrid');
            if (res.data.length === 0) {
                container.innerHTML = `
                    <div class="empty-state" style="grid-column: 1 / -1; margin-top: 40px;">
                        <div class="empty-icon">&#128218;</div>
                        <h3>No playlists found</h3>
                        <p>Create a playlist to organize your music</p>
                    </div>`;
                return;
            }
            container.innerHTML = res.data.map(pl => {
                const songCount = pl.songs ? pl.songs.length : 0;
                return `
                    <div class="playlist-row" onclick="viewPlaylistDetails(${pl.id})">
                        <div class="playlist-info">
                            <h4>${escapeHtml(pl.name)}</h4>
                            <p>${escapeHtml(pl.description || '')}</p>
                        </div>
                        <div class="playlist-stats">
                            ${songCount} song${songCount !== 1 ? 's' : ''}
                        </div>
                        <button class="btn-icon" onclick="event.stopPropagation(); deletePlaylist(${pl.id})" title="Delete Playlist">&#10005;</button>
                    </div>
                `;
            }).join('');
            // Switch class from song-grid to standard block
            container.className = '';
        }
    } catch (e) {
        console.error('Failed to load playlists', e);
    }
}

function openCreatePlaylistModal() {
    document.getElementById('createPlaylistModal').classList.add('show');
}

function closeCreatePlaylistModal() {
    document.getElementById('createPlaylistModal').classList.remove('show');
    document.getElementById('createPlaylistForm').reset();
}

async function handleCreatePlaylist(event) {
    event.preventDefault();
    const btn = document.getElementById('createPlaylistBtn');
    const name = document.getElementById('playlistName').value.trim();
    const desc = document.getElementById('playlistDesc').value.trim();
    const isPublic = document.getElementById('playlistPublic').checked;

    btn.disabled = true;
    try {
        const res = await API.createPlaylist(name, desc, isPublic);
        if (res.success) {
            showToast('Playlist created', 'success');
            closeCreatePlaylistModal();
            loadPlaylists();
        } else {
            showToast(res.message, 'error');
        }
    } catch (e) {
        showToast(e.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

async function deletePlaylist(id) {
    if (!confirm('Are you sure you want to delete this playlist?')) return;
    try {
        const res = await API.deletePlaylist(id);
        if (res.success) {
            showToast('Playlist deleted', 'success');
            loadPlaylists();
        }
    } catch (e) {
        showToast(e.message, 'error');
    }
}

// ==== Playlist Details ==== //

async function viewPlaylistDetails(id) {
    try {
        const res = await API.getPlaylist(id);
        if (res.success && res.data) {
            const pl = res.data;
            window._currentPlaylist = pl;

            document.getElementById('librarySection').style.display = 'none';
            const detailsSection = document.getElementById('playlistDetailsSection');
            detailsSection.style.display = 'block';

            document.getElementById('playlistDetailName').textContent = pl.name;
            document.getElementById('playlistDetailDesc').textContent = pl.description || '';

            const songs = pl.songs || [];
            window._playlistSongs = songs;
            renderSongGrid('playlistSongsGrid', songs);

            // Add remove buttons to each song card in the playlist
            const grid = document.getElementById('playlistSongsGrid');
            const cards = grid.querySelectorAll('.song-card');
            cards.forEach((card, index) => {
                const songId = songs[index] ? songs[index].id : null;
                if (songId) {
                    const removeBtn = document.createElement('button');
                    removeBtn.className = 'btn-remove-song';
                    removeBtn.innerHTML = '&#10005;';
                    removeBtn.title = 'Remove from playlist';
                    removeBtn.onclick = function (e) {
                        e.stopPropagation();
                        removeSongFromCurrentPlaylist(songId);
                    };
                    card.style.position = 'relative';
                    card.appendChild(removeBtn);
                }
            });

            if (songs.length === 0) {
                document.getElementById('playlistSongsGrid').innerHTML = `
                    <div class="empty-state" style="grid-column: 1 / -1; margin-top: 40px;">
                        <div class="empty-icon">&#128266;</div>
                        <h3>Playlist is empty</h3>
                        <p>Add some songs to get started</p>
                    </div>`;
            }
        }
    } catch (e) {
        showToast(e.message, 'error');
    }
}

async function removeSongFromCurrentPlaylist(songId) {
    if (!window._currentPlaylist) return;
    if (!confirm('Remove this song from the playlist?')) return;
    try {
        const res = await API.removeSongFromPlaylist(window._currentPlaylist.id, songId);
        if (res.success) {
            showToast('Song removed from playlist', 'success');
            viewPlaylistDetails(window._currentPlaylist.id);
        }
    } catch (e) {
        showToast(e.message || 'Failed to remove song', 'error');
    }
}

function openAddSongsModal() {
    document.getElementById('addSongsModal').classList.add('show');
    document.getElementById('addSongSearchInput').value = '';
    document.getElementById('addSongsResults').innerHTML = '';
}

function closeAddSongsModal() {
    document.getElementById('addSongsModal').classList.remove('show');
}

// Setup search inside add songs modal
document.getElementById('addSongSearchInput').addEventListener('input', (e) => {
    const query = e.target.value.trim();
    if (!query) {
        document.getElementById('addSongsResults').innerHTML = '';
        return;
    }

    clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(async () => {
        try {
            const res = await API.searchSongs(query);
            if (res.success && res.data) {
                const resultsContainer = document.getElementById('addSongsResults');
                if (res.data.length === 0) {
                    resultsContainer.innerHTML = '<p style="text-align: center; color: var(--text-muted);">No songs found</p>';
                    return;
                }
                resultsContainer.innerHTML = res.data.map(song => {
                    return `
                        <div class="playlist-row" style="margin-bottom: 8px;">
                            <div class="playlist-info">
                                <h4>${escapeHtml(song.title)}</h4>
                                <p>${escapeHtml(song.artistName)}</p>
                            </div>
                            <button class="btn btn-primary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="addSongToCurrentPlaylist(${song.id}, this)">Add</button>
                        </div>
                    `;
                }).join('');
            }
        } catch (e) {
            console.error('Search error', e);
        }
    }, 400);
});

async function addSongToCurrentPlaylist(songId, btnEl) {
    if (!window._currentPlaylist) return;
    try {
        btnEl.disabled = true;
        const res = await API.addSongToPlaylist(window._currentPlaylist.id, songId);
        if (res.success) {
            showToast('Song added to playlist', 'success');
            btnEl.textContent = 'Added';
            btnEl.classList.remove('btn-primary');
            btnEl.classList.add('btn-ghost');
            // silent reload
            viewPlaylistDetails(window._currentPlaylist.id);
        } else {
            showToast(res.message, 'error');
            btnEl.disabled = false;
        }
    } catch (e) {
        showToast(e.message, 'error');
        btnEl.disabled = false;
    }
}

// ==== Album Logic ==== //

// Load albums for the home page (visible to everyone)
async function loadAlbumsHome() {
    try {
        const res = await API.getAllAlbums();
        if (res.success && res.data) {
            window._allAlbums = res.data;
            const container = document.getElementById('albumsGrid');
            if (!container) return;
            if (res.data.length === 0) {
                container.innerHTML = '<p style="color: var(--text-muted); padding: var(--space-md);">No albums yet</p>';
                return;
            }
            renderAlbumGrid('albumsGrid', res.data);
        }
    } catch (e) {
        console.error('Failed to load albums', e);
    }
}

// Load artist's own albums (My Library > My Albums tab)
async function loadMyAlbums() {
    try {
        const res = await API.getMyAlbums();
        if (res.success && res.data) {
            const container = document.getElementById('myAlbumsGrid');
            if (!container) return;
            if (res.data.length === 0) {
                container.innerHTML = `
                    <div class="empty-state" style="margin-top: 40px;">
                        <div class="empty-icon">&#128191;</div>
                        <h3>No albums yet</h3>
                        <p>Create an album to organize your music</p>
                    </div>`;
                return;
            }
            renderAlbumGrid('myAlbumsGrid', res.data, true);
        }
    } catch (e) {
        console.error('Failed to load my albums', e);
    }
}

// Render album cards into a container
function renderAlbumGrid(containerId, albums, showDelete) {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = albums.map(album => {
        const deleteBtn = showDelete
            ? `<button class="btn-remove-song" onclick="event.stopPropagation(); deleteAlbum(${album.id})" title="Delete Album">&#10005;</button>`
            : '';

        let coverHtml = `<div class="cover-placeholder">&#128191;</div>`;
        if (album.coverArt) {
            const coverUrl = API.getAlbumCoverUrl(album.id);
            coverHtml = `<img src="${coverUrl}" alt="Cover" style="width: 100%; height: 100%; object-fit: cover; border-radius: var(--radius-md);">`;
        }

        return `
            <div class="album-card" onclick="viewAlbumDetails(${album.id})" style="position: relative;">
                <div class="album-card-cover" style="padding-top: ${album.coverArt ? '0' : '100%'}; height: ${album.coverArt ? '100%' : 'auto'}; aspect-ratio: 1 / 1;">
                    ${coverHtml}
                </div>
                <div class="album-card-info" style="${album.coverArt ? 'margin-top: var(--space-md);' : ''}">
                    <h4>${escapeHtml(album.name)}</h4>
                    <p>${escapeHtml(album.artistName)} &middot; ${album.songCount} song${album.songCount !== 1 ? 's' : ''}</p>
                </div>
                ${deleteBtn}
            </div>
        `;
    }).join('');
}

// View album details (shows songs inside the album)
async function viewAlbumDetails(id) {
    try {
        const res = await API.getAlbum(id);
        if (res.success && res.data) {
            const album = res.data;
            window._currentAlbum = album;

            // Hide all other sections
            document.getElementById('heroSection').style.display = 'none';
            document.getElementById('genreSection').style.display = 'none';
            document.getElementById('topSongsSection').style.display = 'none';
            document.getElementById('allSongsSection').style.display = 'none';
            document.getElementById('albumsSection').style.display = 'none';
            document.getElementById('searchResultsSection').style.display = 'none';
            document.getElementById('librarySection').style.display = 'none';
            document.getElementById('playlistDetailsSection').style.display = 'none';

            const detailsSection = document.getElementById('albumDetailsSection');
            detailsSection.style.display = 'block';

            document.getElementById('albumDetailName').textContent = album.name;
            document.getElementById('albumDetailDesc').textContent = album.description || (album.releaseDate ? 'Released: ' + album.releaseDate : '');

            const coverImg = document.getElementById('albumDetailCover');
            if (album.coverArt) {
                coverImg.src = API.getAlbumCoverUrl(album.id);
                coverImg.style.display = 'block';
            } else {
                coverImg.style.display = 'none';
            }

            // Only show Add Songs, Upload Song, and Delete buttons if the album belongs to the current user
            const user = API.getUser();
            const isOwner = user && Number(user.userId) === Number(album.artistId);
            document.getElementById('uploadSongToAlbumBtn').style.display = isOwner ? 'inline-flex' : 'none';
            document.getElementById('addSongsToAlbumBtn').style.display = isOwner ? 'inline-flex' : 'none';
            document.getElementById('deleteAlbumBtn').style.display = isOwner ? 'inline-flex' : 'none';

            const songs = album.songs || [];
            renderSongGrid('albumSongsGrid', songs);

            // Add remove buttons to each song card if owner
            if (isOwner && songs.length > 0) {
                const grid = document.getElementById('albumSongsGrid');
                const cards = grid.querySelectorAll('.song-card');
                cards.forEach((card, index) => {
                    const songId = songs[index] ? songs[index].id : null;
                    if (songId) {
                        const removeBtn = document.createElement('button');
                        removeBtn.className = 'btn-remove-song';
                        removeBtn.innerHTML = '&#10005;';
                        removeBtn.title = 'Remove from album';
                        removeBtn.onclick = function (e) {
                            e.stopPropagation();
                            removeSongFromCurrentAlbum(songId);
                        };
                        card.style.position = 'relative';
                        card.appendChild(removeBtn);
                    }
                });
            }

            if (songs.length === 0) {
                document.getElementById('albumSongsGrid').innerHTML = `
                    <div class="empty-state" style="grid-column: 1 / -1; margin-top: 40px;">
                        <div class="empty-icon">&#128191;</div>
                        <h3>Album is empty</h3>
                        <p>Add some songs to this album</p>
                    </div>`;
            }
        }
    } catch (e) {
        showToast(e.message, 'error');
    }
}

// Create Album Modal
function openCreateAlbumModal() {
    document.getElementById('createAlbumModal').classList.add('show');
}

function closeCreateAlbumModal() {
    document.getElementById('createAlbumModal').classList.remove('show');
    document.getElementById('createAlbumForm').reset();
}

async function handleCreateAlbum(event) {
    event.preventDefault();
    const btn = document.getElementById('createAlbumBtn');
    const name = document.getElementById('albumName').value.trim();
    const desc = document.getElementById('albumDesc').value.trim();
    const releaseDate = document.getElementById('albumReleaseDate').value;
    const coverFile = document.getElementById('albumCoverFileInput').files[0];

    try {
        btn.disabled = true;

        const formData = new FormData();
        formData.append('name', name);
        if (desc) formData.append('description', desc);
        if (releaseDate) formData.append('releaseDate', releaseDate);
        if (coverFile) formData.append('coverArt', coverFile);

        const res = await API.createAlbum(formData);
        if (res.success) {
            showToast('Album created!', 'success');
            closeCreateAlbumModal();
            loadMyAlbums();
            loadAlbumsHome();
        } else {
            showToast(res.message, 'error');
        }
    } catch (e) {
        showToast(e.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

// Delete album
async function deleteAlbum(id) {
    if (!confirm('Are you sure you want to delete this album? (Only works if album is empty)')) return;
    try {
        const res = await API.deleteAlbum(id);
        if (res.success) {
            showToast('Album deleted', 'success');
            loadMyAlbums();
            loadAlbumsHome();
            // If we're in album details, go back
            if (window._currentAlbum && window._currentAlbum.id === id) {
                showLibrary();
                switchLibraryTab('albums');
            }
        } else {
            showToast(res.message, 'error');
        }
    } catch (e) {
        showToast(e.message, 'error');
    }
}

async function deleteCurrentAlbum() {
    if (!window._currentAlbum) return;
    await deleteAlbum(window._currentAlbum.id);
}

// Add Songs to Album Modal
function openAddSongsToAlbumModal() {
    document.getElementById('addSongsToAlbumModal').classList.add('show');
    loadArtistSongsForAlbum();
}

function closeAddSongsToAlbumModal() {
    document.getElementById('addSongsToAlbumModal').classList.remove('show');
}

async function loadArtistSongsForAlbum() {
    const container = document.getElementById('addSongsToAlbumResults');
    if (!container) return;
    container.innerHTML = '<p style="text-align: center; color: var(--text-muted);">Loading...</p>';

    try {
        const user = API.getUser();
        if (!user) return;
        const res = await API.getSongsByArtist(user.userId);
        if (res.success && res.data) {
            const albumSongIds = (window._currentAlbum && window._currentAlbum.songs)
                ? window._currentAlbum.songs.map(s => s.id)
                : [];

            const availableSongs = res.data.filter(s => !albumSongIds.includes(s.id));

            if (availableSongs.length === 0) {
                container.innerHTML = '<p style="text-align: center; color: var(--text-muted);">No songs available to add</p>';
                return;
            }

            container.innerHTML = availableSongs.map(song => `
                <div class="playlist-row" style="margin-bottom: 8px;">
                    <div class="playlist-info">
                        <h4>${escapeHtml(song.title)}</h4>
                        <p>${escapeHtml(song.genre || '')}</p>
                    </div>
                    <button class="btn btn-primary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="addSongToCurrentAlbum(${song.id}, this)">Add</button>
                </div>
            `).join('');
        }
    } catch (e) {
        container.innerHTML = '<p style="text-align: center; color: var(--text-muted);">Failed to load songs</p>';
    }
}

async function addSongToCurrentAlbum(songId, btnEl) {
    if (!window._currentAlbum) return;
    try {
        btnEl.disabled = true;
        const res = await API.addSongToAlbum(window._currentAlbum.id, songId);
        if (res.success) {
            showToast('Song added to album', 'success');
            btnEl.textContent = 'Added';
            btnEl.classList.remove('btn-primary');
            btnEl.classList.add('btn-ghost');
            viewAlbumDetails(window._currentAlbum.id);
        } else {
            showToast(res.message, 'error');
            btnEl.disabled = false;
        }
    } catch (e) {
        showToast(e.message, 'error');
        btnEl.disabled = false;
    }
}

async function removeSongFromCurrentAlbum(songId) {
    if (!window._currentAlbum) return;
    if (!confirm('Remove this song from the album?')) return;
    try {
        const res = await API.removeSongFromAlbum(window._currentAlbum.id, songId);
        if (res.success) {
            showToast('Song removed from album', 'success');
            viewAlbumDetails(window._currentAlbum.id);
        }
    } catch (e) {
        showToast(e.message || 'Failed to remove song', 'error');
    }
}
