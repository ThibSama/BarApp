package com.lebarapp.exception;

/** Thrown when a requested cocktail exists but is deactivated (409). */
public class CocktailUnavailableException extends BusinessException {

    public CocktailUnavailableException(Long cocktailId) {
        super(ApiErrorCode.COCKTAIL_UNAVAILABLE,
                "Le cocktail demandé (id " + cocktailId + ") n'est pas disponible.");
    }
}
