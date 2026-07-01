package com.lebarapp.repository;

import com.lebarapp.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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

    /**
     * Case-insensitive existence pre-check for username uniqueness, backed by the
     * same functional unique index {@code uk_app_user_username_lower}. This is a
     * best-effort guard; the database constraint remains the final authority
     * against a concurrent race.
     */
    @Query("""
            select count(u) > 0 from AppUser u
            where lower(u.username) = lower(:username)
            """)
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Deterministic management listing of every staff account, ordered by
     * display name, then username, then id so the result is stable across calls.
     */
    List<AppUser> findAllByOrderByDisplayNameAscUsernameAscIdAsc();
}
