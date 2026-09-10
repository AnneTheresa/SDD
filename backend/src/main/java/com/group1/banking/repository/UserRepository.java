package com.group1.banking.repository;

import com.group1.banking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsernameIgnoreCase(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByUsername(String username);

    /** Used by JwtAuthenticationFilter to resolve the JWT sub claim (Group 1 UUID). */
    Optional<User> findByExternalSubjectId(String externalSubjectId);
    
}
