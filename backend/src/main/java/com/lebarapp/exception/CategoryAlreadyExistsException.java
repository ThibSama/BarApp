package com.lebarapp.exception;

/**
 * Thrown when creating/renaming a category to a name that already exists
 * (case-insensitive uniqueness conflict, 409).
 */
public class CategoryAlreadyExistsException extends BusinessException {

    public CategoryAlreadyExistsException(String name) {
        super(ApiErrorCode.CATEGORY_ALREADY_EXISTS,
                "Une catégorie nommée « " + name + " » existe déjà.");
    }
}
