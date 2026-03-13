/**
 * RevPlay API Client
 * Centralized API communication with JWT token management.
 */
const API = (() => {
    const BASE_URL = '';  // Same origin — Spring Boot serves everything

    // ---- Token Management ----
    function getToken() {
        return localStorage.getItem('revplay_token');
    }

    function setToken(token) {
        localStorage.setItem('revplay_token', token);
    }

    function removeToken() {
        localStorage.removeItem('revplay_token');
    }

    function getUser() {
        const user = localStorage.getItem('revplay_user');
        return user ? JSON.parse(user) : null;
    }

    function setUser(user) {
        localStorage.setItem('revplay_user', JSON.stringify(user));
    }

    function removeUser() {
        localStorage.removeItem('revplay_user');
    }

    function isLoggedIn() {
        return !!getToken();
    }

    // ---- HTTP Helpers ----
    function authHeaders() {
        const headers = {};
        const token = getToken();
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }
        return headers;
    }

    async function request(method, path, body, isFormData) {
        const url = BASE_URL + path;
        const options = {
            method: method,
            headers: { ...authHeaders() }
        };

        if (body) {
            if (isFormData) {
                // Let browser set Content-Type with boundary for FormData
                options.body = body;
            } else {
                options.headers['Content-Type'] = 'application/json';
                options.body = JSON.stringify(body);
            }
        }

        const response = await fetch(url, options);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Request failed');
        }

        return data;
    }

    // ---- Auth Endpoints ----
    async function login(usernameOrEmail, password) {
        const res = await request('POST', '/api/auth/login', {
            usernameOrEmail,
            password
        });
        if (res.success && res.data) {
            setToken(res.data.token);
            setUser({
                userId: res.data.userId,
                username: res.data.username,
                email: res.data.email,
                role: res.data.role,
                displayName: res.data.displayName,
                profilePicture: res.data.profilePicture
            });
        }
        return res;
    }

    async function register(userData) {
        const res = await request('POST', '/api/auth/register', userData);
        if (res.success && res.data) {
            setToken(res.data.token);
            setUser({
                userId: res.data.userId,
                username: res.data.username,
                email: res.data.email,
                role: res.data.role,
                displayName: res.data.displayName,
                profilePicture: res.data.profilePicture
            });
        }
        return res;
    }

    function logout() {
        removeToken();
        removeUser();
    }

    // ---- Song Endpoints ----
    async function getSongs() {
        return request('GET', '/api/songs');
    }

    async function getSongById(id) {
        return request('GET', '/api/songs/' + id);
    }

    async function getTopSongs() {
        return request('GET', '/api/songs/top');
    }

    async function searchSongs(query) {
        return request('GET', '/api/songs/search?query=' + encodeURIComponent(query));
    }

    async function getGenres() {
        return request('GET', '/api/songs/genres');
    }

    async function getSongsByArtist(artistId) {
        return request('GET', '/api/songs/artist/' + artistId);
    }

    async function uploadSong(formData) {
        return request('POST', '/api/songs/upload', formData, true);
    }

    async function deleteSong(id) {
        return request('DELETE', '/api/songs/' + id);
    }

    function getStreamUrl(songId) {
        return BASE_URL + '/api/songs/' + songId + '/stream';
    }

    function getCoverUrl(songId) {
        return BASE_URL + '/api/songs/' + songId + '/cover';
    }

    // ---- Favorites Endpoints ----
    async function toggleFavorite(songId) {
        return request('POST', '/api/favorites/toggle/' + songId);
    }

    async function getMyFavorites() {
        return request('GET', '/api/favorites/my');
    }

    // ---- Playlist Endpoints ----
    async function getMyPlaylists() {
        return request('GET', '/api/playlists/my');
    }

    async function createPlaylist(name, description, isPublic = true) {
        return request('POST', '/api/playlists', { name, description, isPublic });
    }

    async function getPlaylist(playlistId) {
        return request('GET', '/api/playlists/' + playlistId);
    }

    async function addSongToPlaylist(playlistId, songId) {
        return request('POST', '/api/playlists/' + playlistId + '/songs/' + songId);
    }

    async function removeSongFromPlaylist(playlistId, songId) {
        return request('DELETE', '/api/playlists/' + playlistId + '/songs/' + songId);
    }

    async function deletePlaylist(playlistId) {
        return request('DELETE', '/api/playlists/' + playlistId);
    }
    // ---- Albums ----
    async function getAllAlbums() {
        return request('GET', '/api/albums');
    }

    async function getAlbum(id) {
        return request('GET', '/api/albums/' + id);
    }

    async function getMyAlbums() {
        return request('GET', '/api/albums/my');
    }

    async function createAlbum(formData) {
        return request('POST', '/api/albums', formData, true);
    }

    async function updateAlbum(id, formData) {
        return request('PUT', '/api/albums/' + id, formData, true);
    }

    function getAlbumCoverUrl(albumId) {
        return `/api/albums/${albumId}/cover`;
    }

    async function deleteAlbum(id) {
        return request('DELETE', '/api/albums/' + id);
    }

    async function addSongToAlbum(albumId, songId) {
        return request('POST', '/api/albums/' + albumId + '/songs/' + songId);
    }

    async function removeSongFromAlbum(albumId, songId) {
        return request('DELETE', '/api/albums/' + albumId + '/songs/' + songId);
    }

    // ---- Public API ----
    return {
        getToken,
        getUser,
        isLoggedIn,
        login,
        register,
        logout,
        getSongs,
        getSongById,
        getTopSongs,
        searchSongs,
        getGenres,
        getSongsByArtist,
        uploadSong,
        deleteSong,
        getStreamUrl,
        getCoverUrl,
        getAlbumCoverUrl,
        toggleFavorite,
        getMyFavorites,
        getMyPlaylists,
        createPlaylist,
        getPlaylist,
        addSongToPlaylist,
        removeSongFromPlaylist,
        deletePlaylist,
        getAllAlbums,
        getAlbum,
        getMyAlbums,
        createAlbum,
        updateAlbum,
        deleteAlbum,
        addSongToAlbum,
        removeSongFromAlbum
    };
})();
