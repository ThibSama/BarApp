package com.lebarapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create/update payload for a cocktail in the protected barmaker catalogue API.
 *
 * <p>The cocktail must reference an existing (active) category, carry at least
 * one ingredient and exactly three prices (one per size S, M and L). Names are
 * trimmed server-side; {@code active} defaults to {@code true} when omitted.
 * Cross-line rules (unique ingredient names, exactly one price per size) are
 * enforced in the service.</p>
 */
public record CocktailRequest(
        @NotNull(message = "La catégorie est obligatoire.")
        @Positive(message = "L'identifiant de catégorie doit être positif.")
        Long categoryId,

        @NotBlank(message = "Le nom du cocktail est obligatoire.")
        @Size(max = 150, message = "Le nom du cocktail ne doit pas dépasser 150 caractères.")
        String name,

        @NotBlank(message = "La description est obligatoire.")
        String description,

        @Size(max = 255, message = "Le résumé ne doit pas dépasser 255 caractères.")
        String shortDescription,

        @Size(max = 500, message = "L'URL de l'image ne doit pas dépasser 500 caractères.")
        String imageUrl,

        Boolean active,

        @NotNull(message = "Au moins un ingrédient est obligatoire.")
        @Size(min = 1, message = "Un cocktail doit contenir au moins un ingrédient.")
        List<@NotNull @Valid CocktailIngredientRequest> ingredients,

        @NotNull(message = "Les trois tailles (S, M, L) sont obligatoires.")
        @Size(min = 3, max = 3, message = "Exactement trois prix (S, M, L) sont requis.")
        List<@NotNull @Valid CocktailPriceRequest> prices) {
}
