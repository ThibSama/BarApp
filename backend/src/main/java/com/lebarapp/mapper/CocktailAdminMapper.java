package com.lebarapp.mapper;

import com.lebarapp.dto.CocktailIngredientResponse;
import com.lebarapp.dto.CocktailPriceResponse;
import com.lebarapp.dto.CocktailResponse;
import com.lebarapp.entity.Cocktail;
import com.lebarapp.entity.CocktailIngredient;
import com.lebarapp.entity.CocktailPrice;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Explicit mapping from the {@link Cocktail} aggregate to its management DTO.
 *
 * <p>Ingredients and prices are supplied explicitly rather than read back from
 * the entity's lazy collections: after a write the service hands over the
 * freshly built child rows (the persistence context still holds the pre-update
 * collection), while read paths pass the eagerly fetched associations. The
 * mapper always applies the deterministic presentation order: ingredients by
 * {@code displayOrder} then name, prices by the natural size order (S, M, L).</p>
 */
@Component
public class CocktailAdminMapper {

    private static final Comparator<CocktailIngredient> INGREDIENT_ORDER =
            Comparator.comparingInt(CocktailIngredient::getDisplayOrder)
                    .thenComparing(ci -> ci.getIngredient().getName(), String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<CocktailPrice> PRICE_ORDER =
            Comparator.comparing(CocktailPrice::getSize);

    public CocktailResponse toResponse(Cocktail cocktail,
                                       Collection<CocktailIngredient> ingredients,
                                       Collection<CocktailPrice> prices) {
        List<CocktailIngredientResponse> ingredientDtos = ingredients.stream()
                .sorted(INGREDIENT_ORDER)
                .map(this::toIngredientDto)
                .toList();

        List<CocktailPriceResponse> priceDtos = prices.stream()
                .sorted(PRICE_ORDER)
                .map(this::toPriceDto)
                .toList();

        return new CocktailResponse(
                cocktail.getId(),
                cocktail.getCategory().getId(),
                cocktail.getCategory().getName(),
                cocktail.getName(),
                cocktail.getDescription(),
                cocktail.getShortDescription(),
                cocktail.getImageUrl(),
                cocktail.isActive(),
                ingredientDtos,
                priceDtos);
    }

    /** Read-path overload: maps directly from the entity's fetched associations. */
    public CocktailResponse toResponse(Cocktail cocktail) {
        return toResponse(cocktail, cocktail.getIngredients(), cocktail.getPrices());
    }

    private CocktailIngredientResponse toIngredientDto(CocktailIngredient ci) {
        return new CocktailIngredientResponse(
                ci.getIngredient().getId(),
                ci.getIngredient().getName(),
                ci.getQuantityLabel(),
                ci.getDisplayOrder());
    }

    private CocktailPriceResponse toPriceDto(CocktailPrice price) {
        return new CocktailPriceResponse(price.getSize(), price.getPrice());
    }
}
