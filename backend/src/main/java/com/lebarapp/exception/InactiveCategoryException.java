package com.lebarapp.exception;

/**
 * Thrown when a cocktail is created under (or moved to) an inactive category.
 * Attaching catalogue content to a deactivated category is an unsafe state
 * conflict (409): the cocktail would never surface on the public menu.
 */
public class InactiveCategoryException extends BusinessException {

    public InactiveCategoryException(Long categoryId) {
        super(ApiErrorCode.CATEGORY_INACTIVE,
                "La catégorie (id " + categoryId + ") est inactive : impossible d'y rattacher un cocktail.");
    }
}
