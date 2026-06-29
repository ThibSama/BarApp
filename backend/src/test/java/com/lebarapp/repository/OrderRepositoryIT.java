package com.lebarapp.repository;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.CreateOrderItemRequest;
import com.lebarapp.dto.CreateOrderRequest;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.entity.OrderItem;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-level checks for order retrieval. {@code findWithItemsById} uses an
 * entity graph, so the items collection is fetched in the same query and remains
 * accessible after the persistence context closes (no lazy-loading failure, no
 * per-item N+1). The order's own row plus one query for its items is the bounded
 * fetch strategy.
 */
class OrderRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Test
    void findWithItemsByIdReturnsOrderAndItemsWithoutLazyFailure() {
        OrderResponse created = orderService.createOrder(new CreateOrderRequest(List.of(
                new CreateOrderItemRequest(1L, CocktailSize.M),
                new CreateOrderItemRequest(3L, CocktailSize.S))));

        Optional<CustomerOrder> found = customerOrderRepository.findWithItemsById(created.id());

        assertThat(found).isPresent();
        CustomerOrder order = found.get();
        // The collection was fetched by the entity graph: safe to read here even
        // though the surrounding transaction/persistence context has closed.
        List<OrderItem> items = order.getItems();
        assertThat(items).hasSize(2);
        assertThat(items).extracting(OrderItem::getCocktailNameSnapshot)
                .containsExactlyInAnyOrder("Mojito", "Piña Colada");
        assertThat(items).allSatisfy(i -> assertThat(i.getUnitPriceSnapshot()).isNotNull());
    }

    @Test
    void findWithItemsByIdReturnsEmptyForUnknownId() {
        assertThat(customerOrderRepository.findWithItemsById(UUID.randomUUID())).isEmpty();
    }
}
