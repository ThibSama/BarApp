package com.lebarapp.exception;

/** Thrown when a requested cocktail id does not exist in the catalog (404). */
public class CocktailNotFoundException extends BusinessException {

    public CocktailNotFoundException(Long cocktailId) {
        super(ApiErrorCode.COCKTAIL_NOT_FOUND,
                "Le cocktail demandé (id " + cocktailId + ") est introuvable.");
    }
}
