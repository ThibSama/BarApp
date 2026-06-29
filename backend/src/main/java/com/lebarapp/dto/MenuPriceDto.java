package com.lebarapp.dto;

import com.lebarapp.enums.CocktailSize;

import java.math.BigDecimal;

public record MenuPriceDto(
        CocktailSize size,
        BigDecimal price) {
}
