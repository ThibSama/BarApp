package com.lebarapp.dto;

/**
 * Management representation of an ingredient (active and inactive alike),
 * returned by the protected {@code /api/bar/ingredients} endpoints. The domain
 * ingredient only carries an id, a name and an active flag — no description,
 * unit, stock or price exists in the model, so none is exposed.
 */
public record IngredientResponse(
        Long id,
        String name,
        boolean active) {
}
