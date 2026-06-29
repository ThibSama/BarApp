package com.lebarapp.dto;

import com.lebarapp.enums.UserRole;

/**
 * Public representation of the authenticated barmaker. Never exposes the
 * password hash or any internal/entity field.
 */
public record AuthUserDto(
        Long id,
        String username,
        String displayName,
        UserRole role) {
}
