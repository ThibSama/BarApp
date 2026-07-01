package com.lebarapp.mapper;

import com.lebarapp.dto.IngredientResponse;
import com.lebarapp.entity.Ingredient;
import org.springframework.stereotype.Component;

/**
 * Explicit mapping from the {@link Ingredient} entity to its management DTO.
 * Entities never cross this boundary.
 */
@Component
public class IngredientMapper {

    public IngredientResponse toResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.isActive());
    }
}
