package com.lebarapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One ingredient line of a cocktail management request. The ingredient is
 * resolved/created case-insensitively by name; {@code quantityLabel} is
 * optional.
 */
public record CocktailIngredientRequest(
        @NotBlank(message = "Le nom de l'ingrédient est obligatoire.")
        @Size(max = 120, message = "Le nom de l'ingrédient ne doit pas dépasser 120 caractères.")
        String name,

        @Size(max = 80, message = "La quantité ne doit pas dépasser 80 caractères.")
        String quantityLabel,

        @NotNull(message = "L'ordre d'affichage de l'ingrédient est obligatoire.")
        @Min(value = 0, message = "L'ordre d'affichage doit être positif ou nul.")
        Integer displayOrder) {
}
