package com.lebarapp.exception;

/**
 * Thrown when creating/renaming an ingredient to a name that already exists
 * (case-insensitive uniqueness conflict, 409).
 */
public class IngredientAlreadyExistsException extends BusinessException {

    public IngredientAlreadyExistsException(String name) {
        super(ApiErrorCode.INGREDIENT_ALREADY_EXISTS,
                "Un ingrédient nommé « " + name + " » existe déjà.");
    }
}
