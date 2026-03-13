package com.revplay.repository;

import com.revplay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for User entities.
 * By extending JpaRepository, Spring Data JPA automatically provides
 * implementation
 * for all standard CRUD operations (save, findById, findAll, delete) under the
 * hood.
 * The methods defined here (like findByUsername) are magically implemented by
 * Spring
 * simply by reading the method name.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
