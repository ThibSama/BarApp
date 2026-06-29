package com.lebarapp.repository;

import com.lebarapp.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Case-insensitive username lookup backed by the functional unique index
     * {@code uk_app_user_username_lower}. The raw supplied username is never
     * trimmed or modified here; PostgreSQL {@code LOWER()} normalizes both sides
     * so the stored casing is preserved while matching remains case-insensitive.
     */
    @Query("""
            select u from AppUser u
            where lower(u.username) = lower(:username)
            """)
    Optional<AppUser> findByUsernameIgnoreCase(String username);
}
