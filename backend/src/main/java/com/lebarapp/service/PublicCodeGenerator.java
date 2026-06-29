package com.lebarapp.service;

/**
 * Generates short, human-displayable order codes. Implementations must be
 * independently unit-testable; uniqueness against the database is enforced by
 * the caller (collision retry) plus the {@code public_code} unique constraint.
 */
public interface PublicCodeGenerator {

    /** Returns a candidate code of 6 to 8 uppercase alphanumeric characters. */
    String generate();
}
