package com.lebarapp.exception;

/**
 * Thrown when creating a staff account whose username already exists
 * (case-insensitive uniqueness conflict, 409). The user-facing message is
 * deliberately generic and never exposes SQL, the constraint name or the
 * conflicting account.
 */
public class UsernameAlreadyExistsException extends BusinessException {

    public UsernameAlreadyExistsException() {
        super(ApiErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
