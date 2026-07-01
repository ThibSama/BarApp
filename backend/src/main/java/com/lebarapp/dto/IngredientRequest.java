package com.lebarapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for an ingredient in the protected barmaker catalogue
 * API. The name is trimmed server-side; {@code active} defaults to {@code true}
 * when omitted on creation, and drives activation/deactivation on update.
 */
public record IngredientRequest(
        @NotBlank(message = "Le nom de l'ingrédient est obligatoire.")
        @Size(max = 120, message = "Le nom de l'ingrédient ne doit pas dépasser 120 caractères.")
        String name,

        Boolean active) {
}
