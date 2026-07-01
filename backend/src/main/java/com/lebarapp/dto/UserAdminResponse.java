package com.lebarapp.dto;

import com.lebarapp.enums.UserRole;

import java.time.OffsetDateTime;

/**
 * Manager-facing representation of a staff account, returned by the
 * {@code /api/bar/users} endpoints. Deliberately excludes the password hash and
 * every internal/security field — only safe, displayable data crosses this
 * boundary.
 */
public record UserAdminResponse(
        Long id,
        String username,
        String displayName,
        UserRole role,
        boolean active,
        OffsetDateTime createdAt) {
}
