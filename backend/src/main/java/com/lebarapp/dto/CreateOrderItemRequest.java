package com.lebarapp.dto;

import com.lebarapp.enums.CocktailSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * One requested physical drink: a catalog cocktail id and a requested size.
 * Unknown JSON size values are rejected at deserialization time (handled as a
 * malformed request), and {@code null}/non-positive ids fail bean validation.
 */
public record CreateOrderItemRequest(
        @NotNull(message = "L'identifiant du cocktail est obligatoire.")
        @Positive(message = "L'identifiant du cocktail doit être positif.")
        Long cocktailId,

        @NotNull(message = "La taille est obligatoire.")
        CocktailSize size) {
}
