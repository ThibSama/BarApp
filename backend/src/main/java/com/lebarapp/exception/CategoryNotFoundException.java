package com.lebarapp.exception;

/** Thrown when a requested category id does not exist in the catalog (404). */
public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException(Long categoryId) {
        super(ApiErrorCode.CATEGORY_NOT_FOUND,
                "La catégorie demandée (id " + categoryId + ") est introuvable.");
    }
}
