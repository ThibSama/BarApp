package com.lebarapp.mapper;

import com.lebarapp.dto.MenuCategoryDto;
import com.lebarapp.dto.MenuCocktailDto;
import com.lebarapp.dto.MenuIngredientDto;
import com.lebarapp.dto.MenuPriceDto;
import com.lebarapp.entity.Category;
import com.lebarapp.entity.Cocktail;
import com.lebarapp.entity.CocktailIngredient;
import com.lebarapp.entity.CocktailPrice;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Explicit mapping from JPA entities to menu DTOs. Responsible for excluding
 * inactive ingredients/prices and for applying the public presentation order.
 * Entities are never exposed beyond this boundary.
 */
@Component
public class MenuMapper {

    private static final Comparator<MenuIngredientDto> INGREDIENT_ORDER =
            Comparator.comparingInt(MenuIngredientDto::displayOrder)
                    .thenComparing(MenuIngredientDto::name, String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<MenuCocktailDto> COCKTAIL_ORDER =
            Comparator.comparing(MenuCocktailDto::name, String.CASE_INSENSITIVE_ORDER);

    public MenuCategoryDto toCategoryDto(Category category, List<Cocktail> cocktails) {
        List<MenuCocktailDto> cocktailDtos = cocktails.stream()
                .map(this::toCocktailDto)
                .sorted(COCKTAIL_ORDER)
                .toList();
        return new MenuCategoryDto(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                cocktailDtos);
    }

    public MenuCocktailDto toCocktailDto(Cocktail cocktail) {
        List<MenuIngredientDto> ingredients = cocktail.getIngredients().stream()
                .filter(ci -> ci.getIngredient().isActive())
                .map(this::toIngredientDto)
                .sorted(INGREDIENT_ORDER)
                .toList();

        // Sort by the enum's natural order (S, M, L) — its declaration order.
        List<MenuPriceDto> prices = cocktail.getPrices().stream()
                .filter(CocktailPrice::isActive)
                .map(this::toPriceDto)
                .sorted(Comparator.comparing(MenuPriceDto::size))
                .toList();

        return new MenuCocktailDto(
                cocktail.getId(),
                cocktail.getName(),
                cocktail.getDescription(),
                cocktail.getImageUrl(),
                ingredients,
                prices);
    }

    private MenuIngredientDto toIngredientDto(CocktailIngredient ci) {
        return new MenuIngredientDto(
                ci.getIngredient().getId(),
                ci.getIngredient().getName(),
                ci.getQuantityLabel(),
                ci.getDisplayOrder());
    }

    private MenuPriceDto toPriceDto(CocktailPrice price) {
        return new MenuPriceDto(price.getSize(), price.getPrice());
    }
}
