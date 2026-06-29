package com.lebarapp.exception;

import com.lebarapp.enums.CocktailSize;

/** Thrown when the requested size has no price row for the cocktail (409). */
public class SizeUnavailableException extends BusinessException {

    public SizeUnavailableException(Long cocktailId, CocktailSize size) {
        super(ApiErrorCode.SIZE_UNAVAILABLE,
                "La taille " + size + " n'est pas disponible pour le cocktail demandé (id "
                        + cocktailId + ").");
    }
}
