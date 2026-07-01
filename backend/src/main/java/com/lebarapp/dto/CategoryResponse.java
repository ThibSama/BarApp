package com.lebarapp.dto;

/**
 * Management representation of a category (active and inactive alike). Returned
 * by the protected {@code /api/bar/categories} endpoints. No JPA entity or
 * internal timestamp is exposed.
 */
public record CategoryResponse(
        Long id,
        String name,
        String description,
        int displayOrder,
        boolean active) {
}
