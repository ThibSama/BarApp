package com.lebarapp.exception;

/** Thrown when a requested ingredient id does not exist in the catalog (404). */
public class IngredientNotFoundException extends BusinessException {

    public IngredientNotFoundException(Long ingredientId) {
        super(ApiErrorCode.INGREDIENT_NOT_FOUND,
                "L'ingrédient demandé (id " + ingredientId + ") est introuvable.");
    }
}
