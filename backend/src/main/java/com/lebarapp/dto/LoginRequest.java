package com.lebarapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Barmaker login request. The password is never trimmed or modified; it is
 * compared verbatim against the stored BCrypt hash. {@code username} is
 * validated to a non-blank value whose length matches the database column
 * ({@code VARCHAR(80)}).
 */
public record LoginRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(max = 80, message = "Le nom d'utilisateur ne peut pas dépasser 80 caractères.")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password) {
}
