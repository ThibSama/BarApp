package com.lebarapp.dto;

/**
 * One ingredient line of a cocktail in the management API. {@code id} is the
 * shared ingredient id (reused case-insensitively across cocktails).
 */
public record CocktailIngredientResponse(
        Long id,
        String name,
        String quantityLabel,
        int displayOrder) {
}
