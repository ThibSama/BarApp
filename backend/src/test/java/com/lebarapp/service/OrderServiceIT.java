package com.lebarapp.service;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.CreateOrderItemRequest;
import com.lebarapp.dto.CreateOrderRequest;
import com.lebarapp.dto.OrderItemResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.enums.PreparationStatus;
import com.lebarapp.exception.CocktailNotFoundException;
import com.lebarapp.exception.CocktailUnavailableException;
import com.lebarapp.exception.OrderNotFoundException;
import com.lebarapp.exception.PriceUnavailableException;
import com.lebarapp.exception.SizeUnavailableException;
import com.lebarapp.repository.CustomerOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end order business rules against a real PostgreSQL database.
 *
 * <p>Relies on the demo seed: cocktail 1 (Mojito, S/M/L active), cocktail 2
 * (Cosmopolitan, L price inactive), cocktail 3 (Piña Colada), cocktail 4 (Sex on
 * the Beach, no S size), cocktail 6 (inactive). Cocktails created here for the
 * snapshot test live under an inactive category so they never appear in the
 * shared menu fixtures.</p>
 */
class OrderServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private CustomerOrderRepository customerOrderRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void singleItemCreatesOneOrderItemWithInitialState() {
        OrderResponse order = orderService.createOrder(request(item(1L, CocktailSize.M)));

        assertThat(order.id()).isNotNull();
        assertThat(order.publicCode()).isNotBlank();
        assertThat(order.createdAt()).isNotNull();
        assertThat(order.completedAt()).isNull();
        assertThat(order.status()).isEqualTo(OrderStatus.ORDERED);
        assertThat(order.totalAmount()).isEqualByComparingTo("10.50");
        assertThat(order.items()).hasSize(1);

        OrderItemResponse line = order.items().get(0);
        assertThat(line.sequenceNumber()).isEqualTo(1);
        assertThat(line.cocktailName()).isEqualTo("Mojito");
        assertThat(line.size()).isEqualTo(CocktailSize.M);
        assertThat(line.unitPrice()).isEqualByComparingTo("10.50");
        assertThat(line.preparationStatus()).isEqualTo(PreparationStatus.PREPARATION_INGREDIENTS);
        assertThat(line.completedAt()).isNull();
    }

    @Test
    void duplicateEntriesCreateSeparateSequencedItemsAndExactTotal() {
        OrderResponse order = orderService.createOrder(request(
                item(1L, CocktailSize.M),   // 10.50
                item(1L, CocktailSize.M),   // 10.50
                item(3L, CocktailSize.S))); // 9.50

        assertThat(order.items()).hasSize(3);
        assertThat(order.items()).extracting(OrderItemResponse::sequenceNumber)
                .containsExactly(1, 2, 3);
        assertThat(order.items()).extracting(OrderItemResponse::cocktailName)
                .containsExactly("Mojito", "Mojito", "Piña Colada");
        assertThat(order.totalAmount()).isEqualByComparingTo("30.50");
        assertThat(order.totalAmount().scale()).isEqualTo(2);
    }

    @Test
    void unitPricesAlwaysComeFromServerSideCatalog() {
        // The request DTO has no price field; the persisted unit price is the
        // current catalog price, never anything the client could supply.
        OrderResponse order = orderService.createOrder(request(item(1L, CocktailSize.L)));
        assertThat(order.items().get(0).unitPrice()).isEqualByComparingTo("12.50");
    }

    @Test
    void inactiveCocktailIsRejectedWithNoOrderPersisted() {
        long before = customerOrderRepository.count();
        assertThatThrownBy(() -> orderService.createOrder(request(item(6L, CocktailSize.M))))
                .isInstanceOf(CocktailUnavailableException.class);
        assertThat(customerOrderRepository.count()).isEqualTo(before);
    }

    @Test
    void inactivePriceIsRejected() {
        assertThatThrownBy(() -> orderService.createOrder(request(item(2L, CocktailSize.L))))
                .isInstanceOf(PriceUnavailableException.class);
    }

    @Test
    void unavailableSizeIsRejected() {
        assertThatThrownBy(() -> orderService.createOrder(request(item(4L, CocktailSize.S))))
                .isInstanceOf(SizeUnavailableException.class);
    }

    @Test
    void missingCocktailIsRejected() {
        assertThatThrownBy(() -> orderService.createOrder(request(item(999_999L, CocktailSize.M))))
                .isInstanceOf(CocktailNotFoundException.class);
    }

    @Test
    void mixedValidAndInvalidItemsLeaveNoPartialOrder() {
        long before = customerOrderRepository.count();
        assertThatThrownBy(() -> orderService.createOrder(request(
                item(1L, CocktailSize.M),    // valid
                item(6L, CocktailSize.M))))  // invalid (inactive) -> whole tx rolls back
                .isInstanceOf(CocktailUnavailableException.class);
        assertThat(customerOrderRepository.count()).isEqualTo(before);
    }

    @Test
    void trackingReturnsItemsSortedBySequenceNumber() {
        OrderResponse created = orderService.createOrder(request(
                item(3L, CocktailSize.S),   // seq 1
                item(1L, CocktailSize.M))); // seq 2

        OrderResponse tracked = orderService.getOrder(created.id());

        assertThat(tracked.items()).extracting(OrderItemResponse::sequenceNumber)
                .containsExactly(1, 2);
        assertThat(tracked.items()).extracting(OrderItemResponse::cocktailName)
                .containsExactly("Piña Colada", "Mojito");
    }

    @Test
    void snapshotValuesSurviveLaterCatalogModifications() {
        long martiniId = createTestCocktail("Martini", CocktailSize.L, "12.50");

        OrderResponse created = orderService.createOrder(request(item(martiniId, CocktailSize.L)));
        UUID orderId = created.id();
        BigDecimal originalTotal = created.totalAmount();

        // Rename the catalog cocktail and change its current L price.
        jdbcTemplate.update("UPDATE cocktail SET name = 'Dry Martini' WHERE id = ?", martiniId);
        jdbcTemplate.update("UPDATE cocktail_price SET price = 14.00 WHERE cocktail_id = ? AND size = 'L'",
                martiniId);

        OrderResponse tracked = orderService.getOrder(orderId);
        OrderItemResponse line = tracked.items().get(0);

        assertThat(line.cocktailName()).isEqualTo("Martini");
        assertThat(line.size()).isEqualTo(CocktailSize.L);
        assertThat(line.unitPrice()).isEqualByComparingTo("12.50");
        assertThat(tracked.totalAmount()).isEqualByComparingTo(originalTotal);
        assertThat(tracked.totalAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    void generatedPublicCodesAreUnique() {
        List<String> codes = new ArrayList<>();
        IntStream.range(0, 5).forEach(i ->
                codes.add(orderService.createOrder(request(item(1L, CocktailSize.S))).publicCode()));
        assertThat(codes).doesNotContainNull().hasSize(5);
        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    void trackingUnknownOrderThrowsNotFound() {
        assertThatThrownBy(() -> orderService.getOrder(UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // --- helpers -----------------------------------------------------------

    private static CreateOrderRequest request(CreateOrderItemRequest... items) {
        return new CreateOrderRequest(List.of(items));
    }

    private static CreateOrderItemRequest item(Long cocktailId, CocktailSize size) {
        return new CreateOrderItemRequest(cocktailId, size);
    }

    /** Inserts an active cocktail (under an inactive, menu-invisible category) with one price. */
    private long createTestCocktail(String name, CocktailSize size, String price) {
        List<Long> existing = jdbcTemplate.queryForList(
                "SELECT id FROM category WHERE name = 'ZZ Test (hidden)'", Long.class);
        Long categoryId;
        if (existing.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO category (name, display_order, active) VALUES ('ZZ Test (hidden)', 999, false)");
            categoryId = jdbcTemplate.queryForObject(
                    "SELECT id FROM category WHERE name = 'ZZ Test (hidden)'", Long.class);
        } else {
            categoryId = existing.get(0);
        }
        jdbcTemplate.update(
                "INSERT INTO cocktail (category_id, name, description, active) VALUES (?, ?, 'test', true)",
                categoryId, name);
        Long cocktailId = jdbcTemplate.queryForObject(
                "SELECT id FROM cocktail WHERE category_id = ? AND LOWER(name) = LOWER(?)",
                Long.class, categoryId, name);
        jdbcTemplate.update(
                "INSERT INTO cocktail_price (cocktail_id, size, price, active) VALUES (?, ?, ?, true)",
                cocktailId, size.name(), new BigDecimal(price));
        return cocktailId;
    }
}
