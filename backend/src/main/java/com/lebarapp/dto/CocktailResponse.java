package com.lebarapp.dto;

import java.util.List;

/**
 * Management representation of a cocktail (active and inactive alike), returned
 * by the protected {@code /api/bar/cocktails} endpoints. Carries enough data for
 * the future Vue edit form: category id + name, the scalar fields, the ordered
 * ingredients and the S/M/L prices. No JPA entity is ever exposed.
 */
public record CocktailResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        String imageUrl,
        boolean active,
        List<CocktailIngredientResponse> ingredients,
        List<CocktailPriceResponse> prices) {
}
