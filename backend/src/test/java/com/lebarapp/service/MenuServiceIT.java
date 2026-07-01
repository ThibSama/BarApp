package com.lebarapp.service;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.MenuCategoryDto;
import com.lebarapp.dto.MenuCocktailDto;
import com.lebarapp.dto.MenuIngredientDto;
import com.lebarapp.dto.MenuPriceDto;
import com.lebarapp.dto.MenuResponse;
import com.lebarapp.enums.CocktailSize;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end service behaviour against the seeded PostgreSQL catalog: category /
 * cocktail / ingredient / price ordering, exclusion of inactive records and
 * exact decimal mapping.
 */
class MenuServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MenuService menuService;

    @Test
    void categoriesAreReturnedInDisplayOrder() {
        MenuResponse menu = menuService.getMenu();

        assertThat(menu.categories()).extracting(MenuCategoryDto::name)
                .containsExactly("Classiques", "Tropicaux", "Mocktails");
        // The inactive "Promotions expirées" category is never present.
        assertThat(menu.categories()).extracting(MenuCategoryDto::name)
                .doesNotContain("Promotions expirées");
    }

    @Test
    void cocktailsAreSortedByName() {
        MenuCategoryDto classiques = category("Classiques");
        assertThat(classiques.cocktails()).extracting(MenuCocktailDto::name)
                .containsExactly("Cosmopolitan", "Mojito");
    }

    @Test
    void inactiveIngredientIsExcludedAndIngredientsAreOrdered() {
        MenuCocktailDto mojito = cocktail("Classiques", "Mojito");

        assertThat(mojito.ingredients()).extracting(MenuIngredientDto::name)
                .doesNotContain("Colorant de test");
        // Ingredients ordered by association display_order.
        assertThat(mojito.ingredients()).extracting(MenuIngredientDto::displayOrder)
                .isSorted();
        assertThat(mojito.ingredients()).first()
                .extracting(MenuIngredientDto::name).isEqualTo("Rhum blanc");
    }

    @Test
    void inactivePriceIsExcludedAndPricesAreSizeOrdered() {
        MenuCocktailDto cosmopolitan = cocktail("Classiques", "Cosmopolitan");

        // L price is inactive -> only S and M remain, in S, M order.
        assertThat(cosmopolitan.prices()).extracting(MenuPriceDto::size)
                .containsExactly(CocktailSize.S, CocktailSize.M);

        MenuCocktailDto mojito = cocktail("Classiques", "Mojito");
        assertThat(mojito.prices()).extracting(MenuPriceDto::size)
                .containsExactly(CocktailSize.S, CocktailSize.M, CocktailSize.L);
    }

    @Test
    void priceDecimalsArePreservedExactly() {
        MenuCocktailDto mojito = cocktail("Classiques", "Mojito");
        BigDecimal smallPrice = mojito.prices().stream()
                .filter(p -> p.size() == CocktailSize.S)
                .findFirst().orElseThrow().price();

        assertThat(smallPrice).isEqualByComparingTo("8.50");
        assertThat(smallPrice.scale()).isEqualTo(2);
    }

    @Test
    void activeDemoCocktailsExposeLocalImagePaths() {
        List<MenuCocktailDto> all = menuService.getMenu().categories().stream()
                .flatMap(c -> c.cocktails().stream())
                .toList();

        // Every active demo cocktail now has a local image under /images/cocktails/.
        assertThat(all).extracting(MenuCocktailDto::imageUrl)
                .allSatisfy(url -> {
                    assertThat(url).isNotNull();
                    assertThat(url).startsWith("/images/cocktails/");
                });
        // No cocktail exposes the old fake external URL.
        assertThat(all).noneMatch(c -> c.imageUrl() != null && c.imageUrl().contains("example.com"));

        // The inactive cocktail (and its inactive category) stays filtered out.
        assertThat(all).extracting(MenuCocktailDto::name).doesNotContain("Cocktail retiré");

        // Deterministic mapping sanity for two representative cocktails.
        assertThat(cocktail("Classiques", "Mojito").imageUrl())
                .isEqualTo("/images/cocktails/mojito.webp");
        assertThat(cocktail("Classiques", "Cosmopolitan").imageUrl())
                .isEqualTo("/images/cocktails/cosmopolitan.webp");
    }

    private MenuCategoryDto category(String name) {
        return menuService.getMenu().categories().stream()
                .filter(c -> c.name().equals(name))
                .findFirst().orElseThrow();
    }

    private MenuCocktailDto cocktail(String categoryName, String cocktailName) {
        List<MenuCocktailDto> cocktails = category(categoryName).cocktails();
        return cocktails.stream()
                .filter(c -> c.name().equals(cocktailName))
                .findFirst().orElseThrow();
    }
}
