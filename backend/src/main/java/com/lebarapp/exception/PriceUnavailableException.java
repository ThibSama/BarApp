package com.lebarapp.exception;

import com.lebarapp.enums.CocktailSize;

/** Thrown when the requested size exists but its price row is deactivated (409). */
public class PriceUnavailableException extends BusinessException {

    public PriceUnavailableException(Long cocktailId, CocktailSize size) {
        super(ApiErrorCode.PRICE_UNAVAILABLE,
                "Le tarif de la taille " + size + " n'est pas disponible pour le cocktail demandé (id "
                        + cocktailId + ").");
    }
}
