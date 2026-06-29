package com.lebarapp.dto;

public record MenuIngredientDto(
        Long id,
        String name,
        String quantityLabel,
        int displayOrder) {
}
