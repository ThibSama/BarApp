package com.lebarapp.dto;

import com.lebarapp.enums.CocktailSize;

import java.math.BigDecimal;

/** One price line (size + amount) of a cocktail in the management API. */
public record CocktailPriceResponse(
        CocktailSize size,
        BigDecimal price) {
}
