package com.lebarapp.dto;

import java.util.List;

public record MenuCocktailDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        List<MenuIngredientDto> ingredients,
        List<MenuPriceDto> prices) {
}
