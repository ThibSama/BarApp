package com.lebarapp.service;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.CategoryRequest;
import com.lebarapp.dto.CategoryResponse;
import com.lebarapp.dto.CocktailIngredientRequest;
import com.lebarapp.dto.CocktailIngredientResponse;
import com.lebarapp.dto.CocktailPriceRequest;
import com.lebarapp.dto.CocktailRequest;
import com.lebarapp.dto.CocktailResponse;
import com.lebarapp.dto.CreateOrderItemRequest;
import com.lebarapp.dto.CreateOrderRequest;
import com.lebarapp.dto.MenuCocktailDto;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.enums.PaymentMethod;
import com.lebarapp.exception.CategoryNotFoundException;
import com.lebarapp.exception.CocktailAlreadyExistsException;
import com.lebarapp.exception.CocktailNotFoundException;
import com.lebarapp.exception.InactiveCategoryException;
import com.lebarapp.exception.InvalidCatalogRequestException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end cocktail-management behaviour against the seeded PostgreSQL
 * database. Each test runs in a rolled-back transaction and creates its own
 * categories/cocktails (unique names), so the seed-based fixtures of the other
 * IT classes are never affected.
 */
@Transactional
class CocktailAdminServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private CocktailAdminService cocktailService;
    @Autowired
    private CategoryAdminService categoryService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Simulates a request boundary: flushes pending changes and clears the
     * persistence context so a subsequent read/write reloads from the database
     * exactly as a fresh HTTP request would (each endpoint runs in its own
     * session in production).
     */
    private void requestBoundary() {
        entityManager.flush();
        entityManager.clear();
    }

    // --- create ------------------------------------------------------------

    @Test
    void createWithCategoryIngredientsAndPrices() {
        Long categoryId = newCategory("Bar A");
        CocktailResponse created = cocktailService.create(request(categoryId, "Mojito Test",
                List.of(ing("Rhum blanc", "5 cl", 2), ing("Menthe test", "8 feuilles", 1)),
                List.of(price("S", "7.50"), price("M", "9.00"), price("L", "11.00"))));

        assertThat(created.id()).isNotNull();
        assertThat(created.categoryId()).isEqualTo(categoryId);
        assertThat(created.categoryName()).isEqualTo("Bar A");
        assertThat(created.shortDescription()).isEqualTo("résumé");
        assertThat(created.active()).isTrue();
        // Ingredients returned in displayOrder order.
        assertThat(created.ingredients()).extracting(CocktailIngredientResponse::name)
                .containsExactly("Menthe test", "Rhum blanc");
        assertThat(created.prices()).extracting(p -> p.size())
                .containsExactly(CocktailSize.S, CocktailSize.M, CocktailSize.L);
    }

    @Test
    void managementListIncludesActiveAndInactiveCocktails() {
        Long categoryId = newCategory("Bar List");
        CocktailResponse active = cocktailService.create(simple(categoryId, "Active One"));
        CocktailResponse toDisable = cocktailService.create(simple(categoryId, "Hidden One"));
        cocktailService.deactivate(toDisable.id());

        List<CocktailResponse> all = cocktailService.list();
        assertThat(all).extracting(CocktailResponse::name).contains("Active One", "Hidden One");
        assertThat(all).filteredOn(c -> c.id().equals(toDisable.id()))
                .singleElement().extracting(CocktailResponse::active).isEqualTo(false);
        assertThat(active.active()).isTrue();
    }

    @Test
    void detailReturnsFullCocktail() {
        Long categoryId = newCategory("Bar Detail");
        CocktailResponse created = cocktailService.create(simple(categoryId, "Detailed"));
        requestBoundary();
        CocktailResponse detail = cocktailService.getById(created.id());
        assertThat(detail.name()).isEqualTo("Detailed");
        assertThat(detail.prices()).hasSize(3);
    }

    @Test
    void detailUnknownThrows() {
        assertThatThrownBy(() -> cocktailService.getById(999_999L))
                .isInstanceOf(CocktailNotFoundException.class);
    }

    @Test
    void unknownCategoryIsRejected() {
        assertThatThrownBy(() -> cocktailService.create(simple(999_999L, "Orphan")))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void inactiveCategoryIsRejected() {
        // Category id 4 ("Promotions expirées") is seeded inactive.
        assertThatThrownBy(() -> cocktailService.create(simple(4L, "Under Inactive")))
                .isInstanceOf(InactiveCategoryException.class);
    }

    @Test
    void duplicateNameInSameCategoryIsRejected() {
        Long categoryId = newCategory("Bar Dup");
        cocktailService.create(simple(categoryId, "Spritz"));
        assertThatThrownBy(() -> cocktailService.create(simple(categoryId, "spritz")))
                .isInstanceOf(CocktailAlreadyExistsException.class);
    }

    @Test
    void sameNameAllowedInDifferentCategory() {
        Long a = newCategory("Bar X");
        Long b = newCategory("Bar Y");
        cocktailService.create(simple(a, "Negroni"));
        CocktailResponse second = cocktailService.create(simple(b, "Negroni"));
        assertThat(second.categoryId()).isEqualTo(b);
    }

    @Test
    void duplicateIngredientNamesAreRejected() {
        Long categoryId = newCategory("Bar Ing");
        assertThatThrownBy(() -> cocktailService.create(request(categoryId, "BadIng",
                List.of(ing("Rhum", "1", 1), ing("rhum", "2", 2)),
                threePrices())))
                .isInstanceOf(InvalidCatalogRequestException.class);
    }

    @Test
    void duplicatePriceSizeIsRejected() {
        Long categoryId = newCategory("Bar Price Dup");
        assertThatThrownBy(() -> cocktailService.create(request(categoryId, "BadPrice",
                List.of(ing("Rhum", "1", 1)),
                List.of(price("S", "7.50"), price("S", "8.00"), price("L", "11.00")))))
                .isInstanceOf(InvalidCatalogRequestException.class);
    }

    @Test
    void missingSizeIsRejected() {
        Long categoryId = newCategory("Bar Price Missing");
        // Only S and M supplied (bypasses bean validation by calling the service directly).
        assertThatThrownBy(() -> cocktailService.create(request(categoryId, "MissingL",
                List.of(ing("Rhum", "1", 1)),
                List.of(price("S", "7.50"), price("M", "9.00")))))
                .isInstanceOf(InvalidCatalogRequestException.class);
    }

    @Test
    void ingredientIsReusedCaseInsensitivelyAcrossCocktails() {
        Long categoryId = newCategory("Bar Reuse");
        CocktailResponse first = cocktailService.create(request(categoryId, "Reuse One",
                List.of(ing("Verveine test", "1", 1)), threePrices()));
        CocktailResponse second = cocktailService.create(request(categoryId, "Reuse Two",
                List.of(ing("verveine TEST", "2", 1)), threePrices()));

        Long firstIngredientId = first.ingredients().get(0).id();
        Long secondIngredientId = second.ingredients().get(0).id();
        assertThat(secondIngredientId).isEqualTo(firstIngredientId);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ingredient WHERE LOWER(name) = LOWER('Verveine test')", Integer.class);
        assertThat(rows).isEqualTo(1);
    }

    // --- update ------------------------------------------------------------

    @Test
    void updateDetailsModifiesScalarFields() {
        Long categoryId = newCategory("Bar Upd");
        CocktailResponse created = cocktailService.create(simple(categoryId, "Old Name"));
        CocktailResponse updated = cocktailService.update(created.id(), request(categoryId, "New Name",
                List.of(ing("Rhum", "1", 1)), threePrices()));
        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.id()).isEqualTo(created.id());
    }

    @Test
    void changeCategoryReParentsCocktail() {
        Long a = newCategory("Bar From");
        Long b = newCategory("Bar To");
        CocktailResponse created = cocktailService.create(simple(a, "Mover"));
        CocktailResponse moved = cocktailService.update(created.id(), request(b, "Mover",
                List.of(ing("Rhum", "1", 1)), threePrices()));
        assertThat(moved.categoryId()).isEqualTo(b);
    }

    @Test
    void replaceIngredientsLeavesNoOrphanOrDuplicateRows() {
        Long categoryId = newCategory("Bar Replace Ing");
        CocktailResponse created = cocktailService.create(request(categoryId, "Replacer",
                List.of(ing("Rhum", "1", 1), ing("Menthe test", "2", 2)), threePrices()));

        CocktailResponse updated = cocktailService.update(created.id(), request(categoryId, "Replacer",
                List.of(ing("Citron test", "1", 1), ing("Menthe test", "2", 2)), threePrices()));

        assertThat(updated.ingredients()).extracting(CocktailIngredientResponse::name)
                .containsExactly("Citron test", "Menthe test");
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cocktail_ingredient WHERE cocktail_id = ?", Integer.class, created.id());
        assertThat(rows).isEqualTo(2);
    }

    @Test
    void replacePricesUpdatesAmountsInPlace() {
        Long categoryId = newCategory("Bar Replace Price");
        CocktailResponse created = cocktailService.create(request(categoryId, "Pricer",
                List.of(ing("Rhum", "1", 1)),
                List.of(price("S", "7.00"), price("M", "9.00"), price("L", "11.00"))));

        CocktailResponse updated = cocktailService.update(created.id(), request(categoryId, "Pricer",
                List.of(ing("Rhum", "1", 1)),
                List.of(price("S", "8.25"), price("M", "10.25"), price("L", "12.25"))));

        assertThat(updated.prices()).hasSize(3);
        BigDecimal small = updated.prices().stream()
                .filter(p -> p.size() == CocktailSize.S).findFirst().orElseThrow().price();
        assertThat(small).isEqualByComparingTo("8.25");
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cocktail_price WHERE cocktail_id = ?", Integer.class, created.id());
        assertThat(rows).isEqualTo(3);
    }

    // --- logical deletion & menu compatibility -----------------------------

    @Test
    void deactivatedCocktailDisappearsFromMenuButStaysInManagement() {
        Long categoryId = newCategory("Bar Menu");
        CocktailResponse created = cocktailService.create(simple(categoryId, "Menu Drink"));
        assertThat(menuCocktailNames()).contains("Menu Drink");

        cocktailService.deactivate(created.id());
        assertThat(menuCocktailNames()).doesNotContain("Menu Drink");
        assertThat(cocktailService.list()).extracting(CocktailResponse::name).contains("Menu Drink");
    }

    @Test
    void reactivationThroughUpdateRestoresMenuVisibility() {
        Long categoryId = newCategory("Bar Reactivate");
        CocktailResponse created = cocktailService.create(simple(categoryId, "Back Soon"));
        cocktailService.deactivate(created.id());
        assertThat(menuCocktailNames()).doesNotContain("Back Soon");

        cocktailService.update(created.id(), requestActive(categoryId, "Back Soon",
                List.of(ing("Rhum", "1", 1)), threePrices(), true));
        assertThat(menuCocktailNames()).contains("Back Soon");
    }

    @Test
    void existingOrderSnapshotsRemainUnaffectedAfterCocktailChanges() {
        Long categoryId = newCategory("Bar Snapshot");
        CocktailResponse created = cocktailService.create(request(categoryId, "Snapshot Drink",
                List.of(ing("Rhum", "1", 1)),
                List.of(price("S", "7.00"), price("M", "9.00"), price("L", "11.00"))));

        requestBoundary();
        OrderResponse order = orderService.createOrder(new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(created.id(), CocktailSize.M)),
                5, PaymentMethod.CARD_IN_APP));

        // Rename + reprice + deactivate the cocktail after the order exists.
        cocktailService.update(created.id(), request(categoryId, "Renamed Drink",
                List.of(ing("Rhum", "1", 1)),
                List.of(price("S", "20.00"), price("M", "25.00"), price("L", "30.00"))));
        cocktailService.deactivate(created.id());

        OrderResponse tracked = orderService.getOrder(order.id());
        assertThat(tracked.items().get(0).cocktailName()).isEqualTo("Snapshot Drink");
        assertThat(tracked.items().get(0).unitPrice()).isEqualByComparingTo("9.00");
    }

    // --- helpers -----------------------------------------------------------

    private Long newCategory(String name) {
        CategoryResponse category = categoryService.create(new CategoryRequest(name, null, 60, true));
        return category.id();
    }

    private static CocktailIngredientRequest ing(String name, String qty, int order) {
        return new CocktailIngredientRequest(name, qty, order);
    }

    private static CocktailPriceRequest price(String size, String amount) {
        return new CocktailPriceRequest(CocktailSize.valueOf(size), new BigDecimal(amount));
    }

    private static List<CocktailPriceRequest> threePrices() {
        return List.of(price("S", "7.50"), price("M", "9.00"), price("L", "11.00"));
    }

    private static CocktailRequest request(Long categoryId, String name,
                                           List<CocktailIngredientRequest> ingredients,
                                           List<CocktailPriceRequest> prices) {
        return requestActive(categoryId, name, ingredients, prices, null);
    }

    private static CocktailRequest requestActive(Long categoryId, String name,
                                                 List<CocktailIngredientRequest> ingredients,
                                                 List<CocktailPriceRequest> prices, Boolean active) {
        return new CocktailRequest(categoryId, name, "description complète", "résumé",
                "https://example.test/img.jpg", active, ingredients, prices);
    }

    private static CocktailRequest simple(Long categoryId, String name) {
        return request(categoryId, name, List.of(ing("Rhum", "1 cl", 1)), threePrices());
    }

    private List<String> menuCocktailNames() {
        return menuService.getMenu().categories().stream()
                .flatMap(c -> c.cocktails().stream())
                .map(MenuCocktailDto::name)
                .toList();
    }
}
