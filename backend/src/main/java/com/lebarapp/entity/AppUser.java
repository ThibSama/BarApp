package com.lebarapp.entity;

import com.lebarapp.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Staff account (barmaker or manager). Authentication uses Spring Security with
 * BCrypt password hashes and stateless JWTs. New accounts are created only
 * through {@link #createBarmaker(String, String, String)}; the role and password
 * hash have no public setters so they cannot be reassigned after creation.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role = UserRole.BARMAKER;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected AppUser() {
    }

    /**
     * Controlled factory for a brand-new barmaker account. The role is fixed to
     * {@link UserRole#BARMAKER} and the account is created active; neither can be
     * chosen by the caller. The password argument must already be a BCrypt hash —
     * no plaintext password is ever stored. Timestamps are assigned by the
     * database ({@code insertable = false}).
     *
     * @param username     login identifier (already validated/trimmed by the service)
     * @param passwordHash a BCrypt hash, never a plaintext password
     * @param displayName  human-readable staff name (already validated/trimmed)
     */
    public static AppUser createBarmaker(String username, String passwordHash, String displayName) {
        AppUser user = new AppUser();
        user.username = username;
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.role = UserRole.BARMAKER;
        user.active = true;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppUser other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
