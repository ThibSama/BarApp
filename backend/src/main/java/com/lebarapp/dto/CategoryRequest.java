package com.lebarapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for a category in the protected barmaker catalogue API.
 * Surrounding whitespace on {@code name}/{@code description} is trimmed
 * server-side; {@code active} defaults to {@code true} when omitted.
 */
public record CategoryRequest(
        @NotBlank(message = "Le nom de la catégorie est obligatoire.")
        @Size(max = 100, message = "Le nom de la catégorie ne doit pas dépasser 100 caractères.")
        String name,

        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères.")
        String description,

        @NotNull(message = "L'ordre d'affichage est obligatoire.")
        @Min(value = 0, message = "L'ordre d'affichage doit être positif ou nul.")
        Integer displayOrder,

        Boolean active) {
}
