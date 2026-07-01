package com.lebarapp.exception;

/**
 * Thrown when creating/renaming a cocktail to a name already used within the
 * same category (case-insensitive uniqueness conflict, 409).
 */
public class CocktailAlreadyExistsException extends BusinessException {

    public CocktailAlreadyExistsException(String name) {
        super(ApiErrorCode.COCKTAIL_ALREADY_EXISTS,
                "Un cocktail nommé « " + name + " » existe déjà dans cette catégorie.");
    }
}
