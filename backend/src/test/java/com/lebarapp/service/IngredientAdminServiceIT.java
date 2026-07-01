package com.lebarapp.service;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.CategoryRequest;
import com.lebarapp.dto.CocktailIngredientRequest;
import com.lebarapp.dto.CocktailIngredientResponse;
import com.lebarapp.dto.CocktailPriceRequest;
import com.lebarapp.dto.CocktailRequest;
import com.lebarapp.dto.CocktailResponse;
import com.lebarapp.dto.IngredientRequest;
import com.lebarapp.dto.IngredientResponse;
import com.lebarapp.dto.MenuCocktailDto;
import com.lebarapp.dto.MenuIngredientDto;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.exception.IngredientAlreadyExistsException;
import com.lebarapp.exception.IngredientNotFoundException;
import com.lebarapp.repository.IngredientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end ingredient-administration behaviour against the seeded PostgreSQL
 * database, including compatibility with the cocktail aggregate and the public
 * menu. Each test runs in a rolled-back transaction so seeded rows used by the
 * other IT classes are never mutated for real.
 */
@Transactional
class IngredientAdminServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private IngredientAdminService ingredientService;
    @Autowired
    private CategoryAdminService categoryService;
    @Autowired
    private CocktailAdminService cocktailService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // --- CRUD --------------------------------------------------------------

    @Test
    void listIncludesActiveAndInactiveIngredients() {
        List<String> names = ingredientService.list().stream().map(IngredientResponse::name).toList();
        assertThat(names).contains("Rhum blanc", "Colorant de test"); // seed: active + inactive
    }

    @Test
    void listIsOrderedActiveFirstThenName() {
        List<IngredientResponse> ingredients = ingredientService.list();
        assertThat(ingredients).isSortedAccordingTo(
                Comparator.comparing(IngredientResponse::active).reversed()
                        .thenComparing(IngredientResponse::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(IngredientResponse::id));
    }

    @Test
    void createPersistsIngredient() {
        IngredientResponse created = ingredientService.create(new IngredientRequest("Basilic test", null));
        assertThat(created.id()).isNotNull();
        assertThat(created.active()).isTrue();
        assertThat(ingredientService.list()).extracting(IngredientResponse::name).contains("Basilic test");
    }

    @Test
    void createTrimsNameAndDefaultsActive() {
        IngredientResponse created = ingredientService.create(new IngredientRequest("  Gingembre test  ", null));
        assertThat(created.name()).isEqualTo("Gingembre test");
        assertThat(created.active()).isTrue();
    }

    @Test
    void createCanStartInactive() {
        IngredientResponse created = ingredientService.create(new IngredientRequest("Sirop test", false));
        assertThat(created.active()).isFalse();
    }

    @Test
    void duplicateNameRejectedCaseInsensitively() {
        assertThatThrownBy(() -> ingredientService.create(new IngredientRequest("rhum BLANC", null)))
                .isInstanceOf(IngredientAlreadyExistsException.class);
    }

    @Test
    void updateRenamesIngredient() {
        IngredientResponse created = ingredientService.create(new IngredientRequest("Cannelle test", null));
        IngredientResponse updated = ingredientService.update(created.id(),
                new IngredientRequest("Cannelle moulue test", true));
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Cannelle moulue test");
    }

    @Test
    void updateUnknownIdThrows() {
        assertThatThrownBy(() -> ingredientService.update(999_999L, new IngredientRequest("X", true)))
                .isInstanceOf(IngredientNotFoundException.class);
    }

    @Test
    void updateSupportsDeactivationAndReactivation() {
        IngredientResponse created = ingredientService.create(new IngredientRequest("Anis test", null));
        IngredientResponse off = ingredientService.update(created.id(), new IngredientRequest("Anis test", false));
        assertThat(off.active()).isFalse();
        IngredientResponse on = ingredientService.update(created.id(), new IngredientRequest("Anis test", true));
        assertThat(on.active()).isTrue();
    }

    @Test
    void updateToExistingNameIsRejected() {
        IngredientResponse a = ingredientService.create(new IngredientRequest("Alpha test", null));
        ingredientService.create(new IngredientRequest("Beta test", null));
        assertThatThrownBy(() -> ingredientService.update(a.id(), new IngredientRequest("beta TEST", true)))
                .isInstanceOf(IngredientAlreadyExistsException.class);
    }

    @Test
    void deactivateUnknownIdThrows() {
        assertThatThrownBy(() -> ingredientService.deactivate(999_999L))
                .isInstanceOf(IngredientNotFoundException.class);
    }

    @Test
    void deactivateIsLogicalAndIdempotentWithoutPhysicalDeletion() {
        IngredientResponse created = ingredientService.create(new IngredientRequest("Verveine test", null));
        long before = ingredientRepository.count();

        ingredientService.deactivate(created.id());
        ingredientService.deactivate(created.id()); // idempotent: no error, still inactive

        // Row is retained (logical deletion only).
        assertThat(ingredientRepository.count()).isEqualTo(before);
        assertThat(ingredientRepository.findById(created.id())).isPresent()
                .get().extracting(i -> i.isActive()).isEqualTo(false);
        assertThat(ingredientService.list()).extracting(IngredientResponse::name).contains("Verveine test");
    }

    // --- compatibility with cocktails & menu -------------------------------

    @Test
    void deactivatingIngredientPreservesCocktailRelationships() {
        // Seed cocktail 1 (Mojito) uses ingredient 1 (Rhum blanc).
        long associationsBefore = countAssociations(1L);
        ingredientService.deactivate(1L);

        assertThat(countAssociations(1L)).isEqualTo(associationsBefore);
        assertThat(cocktailService.getById(1L).name()).isEqualTo("Mojito");
    }

    @Test
    void cocktailCreationReusesExistingIngredientCaseInsensitively() {
        Long categoryId = categoryService.create(new CategoryRequest("Bar Ing IT", null, 80, true)).id();
        Long seedRhumId = ingredientRepository.findByNameIgnoreCase("Rhum blanc").orElseThrow().getId();
        long ingredientCountBefore = ingredientRepository.count();

        CocktailResponse cocktail = cocktailService.create(request(categoryId, "Reuse Drink",
                List.of(new CocktailIngredientRequest("rhum BLANC", "5 cl", 1))));

        assertThat(cocktail.ingredients()).extracting(CocktailIngredientResponse::id).containsExactly(seedRhumId);
        assertThat(ingredientRepository.count()).isEqualTo(ingredientCountBefore); // no new row
    }

    @Test
    void inactiveIngredientIsReactivatedWhenReusedByCocktail() {
        Long categoryId = categoryService.create(new CategoryRequest("Bar React IT", null, 81, true)).id();
        Long seedSugarId = ingredientRepository.findByNameIgnoreCase("Sucre de canne").orElseThrow().getId();
        ingredientService.deactivate(seedSugarId);
        assertThat(ingredientRepository.findById(seedSugarId).orElseThrow().isActive()).isFalse();

        cocktailService.create(request(categoryId, "React Drink",
                List.of(new CocktailIngredientRequest("sucre de canne", "2 cl", 1))));

        assertThat(ingredientRepository.findById(seedSugarId).orElseThrow().isActive()).isTrue();
    }

    @Test
    void sharedIngredientIsNotDuplicatedAcrossCocktails() {
        Long categoryId = categoryService.create(new CategoryRequest("Bar Shared IT", null, 82, true)).id();
        CocktailResponse a = cocktailService.create(request(categoryId, "Shared A",
                List.of(new CocktailIngredientRequest("Hibiscus test", "1", 1))));
        CocktailResponse b = cocktailService.create(request(categoryId, "Shared B",
                List.of(new CocktailIngredientRequest("HIBISCUS TEST", "2", 1))));

        assertThat(b.ingredients().get(0).id()).isEqualTo(a.ingredients().get(0).id());
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ingredient WHERE LOWER(name) = LOWER('Hibiscus test')", Integer.class);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void publicMenuStaysValidAfterIngredientDeactivation() {
        // Seed ingredient 2 (Citron vert) is used by the active Mojito cocktail.
        ingredientService.deactivate(2L);

        List<MenuCocktailDto> cocktails = menuService.getMenu().categories().stream()
                .flatMap(c -> c.cocktails().stream())
                .toList();
        MenuCocktailDto mojito = cocktails.stream()
                .filter(c -> c.name().equals("Mojito")).findFirst().orElseThrow();

        // The cocktail is still present and serializable; the inactive ingredient
        // is simply filtered out of its composition (established menu rule).
        assertThat(mojito.ingredients()).extracting(MenuIngredientDto::name).doesNotContain("Citron vert");
        assertThat(mojito.ingredients()).isNotEmpty();
    }

    // --- helpers -----------------------------------------------------------

    private Integer countAssociations(Long cocktailId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cocktail_ingredient WHERE cocktail_id = ?", Integer.class, cocktailId);
    }

    private static CocktailRequest request(Long categoryId, String name,
                                           List<CocktailIngredientRequest> ingredients) {
        List<CocktailPriceRequest> prices = List.of(
                new CocktailPriceRequest(CocktailSize.S, new BigDecimal("7.50")),
                new CocktailPriceRequest(CocktailSize.M, new BigDecimal("9.00")),
                new CocktailPriceRequest(CocktailSize.L, new BigDecimal("11.00")));
        return new CocktailRequest(categoryId, name, "description",
                null, true, ingredients, prices);
    }
}
