package com.lebarapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Manager-only payload to create a new barmaker account
 * ({@code POST /api/bar/users}).
 *
 * <p>The role is intentionally absent from this contract: every account created
 * here is fixed server-side to {@code BARMAKER} and always active. The payload
 * carries only a display name, a login username and a plaintext password (hashed
 * server-side, never stored or logged in clear).</p>
 *
 * <p>{@code displayName} and {@code username} are trimmed server-side before
 * persistence; the password is used verbatim (never trimmed) so that leading or
 * trailing spaces remain part of the secret.</p>
 */
public record CreateBarmakerRequest(
        @NotBlank(message = "Le nom affiché est obligatoire.")
        @Size(max = 120, message = "Le nom affiché ne doit pas dépasser 120 caractères.")
        String displayName,

        @NotBlank(message = "Le nom d’utilisateur est obligatoire.")
        @Size(min = 3, max = 80, message = "Le nom d’utilisateur doit contenir entre 3 et 80 caractères.")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "Le nom d’utilisateur ne peut contenir que des lettres, chiffres, points, tirets et underscores.")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
        String password) {
}
