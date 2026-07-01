package com.lebarapp.dto;

import com.lebarapp.enums.CocktailSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * One price line of a cocktail management request. Exactly three lines (one per
 * size S, M and L) are required; that cross-line rule is enforced in the
 * service. Monetary amounts use {@link BigDecimal} and must be strictly
 * positive.
 */
public record CocktailPriceRequest(
        @NotNull(message = "La taille est obligatoire.")
        CocktailSize size,

        @NotNull(message = "Le prix est obligatoire.")
        @Positive(message = "Le prix doit être strictement positif.")
        BigDecimal price) {
}
