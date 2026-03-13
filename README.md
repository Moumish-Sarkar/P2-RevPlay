# P2-RevPlay
---

# RevPlay – Music Streaming Backend

RevPlay is a backend music streaming platform built using **Spring Boot**. The system allows users to upload songs, create playlists, like songs, and manage artist profiles. It follows a **layered architecture (Controller → Service → Repository)** and implements **JWT-based authentication** for secure API access. 

This project demonstrates backend development concepts such as **REST APIs, authentication, database design, and scalable architecture**.

---

# Tech Stack

**Backend**

* Java
* Spring Boot
* Spring Security
* JWT Authentication
* Spring Data JPA
* Hibernate ORM

**Database**

* Oracle Database 21c

**Frontend**

* HTML
* CSS
* JavaScript

**Tools**

* Maven
* JUnit
* IntelliJ IDEA

---

# System Architecture

The application follows a **layered architecture**, which separates responsibilities and improves maintainability.

```
Client
   ↓
Controller Layer
   ↓
Service Layer
   ↓
Repository Layer
   ↓
Database
```

### Controller Layer

Handles incoming HTTP requests and maps them to appropriate service methods using REST APIs.

### Service Layer

Contains the core business logic such as authentication, playlist management, and song processing.

### Repository Layer

Interacts directly with the database using **Spring Data JPA repositories**.

---

# Authentication & Security

RevPlay uses **JWT (JSON Web Token)** authentication to secure APIs.

Authentication flow:

1. User sends login request
2. Server validates credentials
3. JWT token is generated
4. Token is sent to the client
5. Client stores the token
6. Client sends the token in the **Authorization header** for every request
7. `JwtAuthenticationFilter` validates the token
8. If valid → request proceeds to controller
9. If invalid → server returns **401 Unauthorized** 

---

# Database Design

The system uses **Oracle Database 21c** with multiple relational tables including:

* USERS
* SONGS
* ALBUMS
* PLAYLISTS
* PLAYLIST_SONGS
* FAVORITES
* LISTENING_HISTORY
* ARTIST_PROFILES
* SOCIAL_LINKS
* PLAYLIST_FOLLOWERS 

These tables manage relationships between users, artists, songs, playlists, and listening activity.

---

# Key Features

### User Features

* Discover and stream songs
* Create playlists
* Like or favorite songs
* Search songs by keywords
* Browse songs by genre

### Artist Features

* Upload songs with metadata
* Create and manage albums
* Maintain artist profiles

### Platform Features

* JWT based authentication
* Song upload and streaming
* Playlist management
* Favorite songs system
* Secure REST APIs 

---

# API Endpoints

### Authentication

```
POST /api/auth/register
POST /api/auth/login
```

### Songs

```
GET    /api/songs
POST   /api/songs/upload
GET    /api/songs/{id}
GET    /api/songs/{id}/stream
GET    /api/songs/{id}/cover
GET    /api/songs/search
GET    /api/songs/top
GET    /api/songs/genres
GET    /api/songs/artist/{artistId}
DELETE /api/songs/{id}
```

### Albums

```
GET    /api/albums
POST   /api/albums
GET    /api/albums/my
GET    /api/albums/{id}
PUT    /api/albums/{id}
DELETE /api/albums/{id}
POST   /api/albums/{id}/songs/{songId}
DELETE /api/albums/{id}/songs/{songId}
```

### Playlists

```
POST   /api/playlists
GET    /api/playlists/my
GET    /api/playlists/{id}
DELETE /api/playlists/{id}
POST   /api/playlists/{id}/songs/{songId}
DELETE /api/playlists/{id}/songs/{songId}
```

### Favorites

```
GET  /api/favorites/my
POST /api/favorites/toggle/{songId}
GET  /api/favorites/check/{songId}
```

---

# Future Enhancements

* Listening history tracking
* Music recommendation system
* Social media integration for artists
* Advanced search and filtering

---

# Conclusion

RevPlay demonstrates a **scalable backend architecture for a music streaming platform** built with Spring Boot and Oracle Database. The project highlights secure authentication, REST API design, and efficient data management for music streaming services. 

