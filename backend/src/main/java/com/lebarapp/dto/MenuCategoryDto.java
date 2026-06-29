package com.lebarapp.dto;

import java.util.List;

public record MenuCategoryDto(
        Long id,
        String name,
        int displayOrder,
        List<MenuCocktailDto> cocktails) {
}
