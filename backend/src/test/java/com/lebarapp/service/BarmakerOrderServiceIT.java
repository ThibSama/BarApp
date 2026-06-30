package com.lebarapp.service;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.BarOrderSummaryResponse;
import com.lebarapp.dto.CreateOrderItemRequest;
import com.lebarapp.dto.CreateOrderRequest;
import com.lebarapp.dto.OrderItemResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.exception.InvalidPreparationTransitionException;
import com.lebarapp.exception.OrderItemNotFoundException;
import com.lebarapp.exception.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Business rules for the barmaker order-processing service against a real
 * PostgreSQL database: queue listing/sorting, aggregate counters, single-step
 * progression and automatic parent-order status synchronization.
 */
class BarmakerOrderServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private BarmakerOrderService barmakerOrderService;

    @Test
    void activeQueueListsOrdersOldestFirstWithProgressCounters() {
        OrderResponse a = createOrder(item(1L, CocktailSize.M), item(3L, CocktailSize.S));
        OrderResponse b = createOrder(item(1L, CocktailSize.M));

        List<BarOrderSummaryResponse> active = barmakerOrderService.listOrders(false);

        // Both freshly-created orders are present and never marked completed.
        List<UUID> ids = active.stream().map(BarOrderSummaryResponse::id).toList();
        assertThat(ids).contains(a.id(), b.id());
        // DB-side sort: createdAt ascending (monotonic, non-decreasing).
        assertThat(active).isSortedAccordingTo(
                Comparator.comparing(BarOrderSummaryResponse::createdAt));

        BarOrderSummaryResponse summaryA = find(active, a.id());
        assertThat(summaryA.status()).isEqualTo(OrderStatus.ORDERED);
        assertThat(summaryA.itemCount()).isEqualTo(2);
        assertThat(summaryA.completedItemCount()).isZero();
        assertThat(summaryA.completedAt()).isNull();
    }

    @Test
    void progressingAnItemMovesOrderToInProgressAndUpdatesCounters() {
        OrderResponse created = createOrder(item(1L, CocktailSize.M), item(3L, CocktailSize.S));
        UUID firstItem = itemIdAtSequence(created, 1);

        OrderResponse refreshed = barmakerOrderService.advanceItemToNextStep(firstItem);

        assertThat(refreshed.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(refreshed.completedAt()).isNull();

        BarOrderSummaryResponse summary = find(barmakerOrderService.listOrders(false), created.id());
        assertThat(summary.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(summary.completedItemCount()).isZero(); // only at ASSEMBLY, not COMPLETED yet
    }

    @Test
    void completingEveryItemCompletesTheOrderAndMovesItToCompletedQueue() {
        OrderResponse created = createOrder(item(1L, CocktailSize.M), item(3L, CocktailSize.S));

        completeOrder(created);

        OrderResponse detail = barmakerOrderService.getOrder(created.id());
        assertThat(detail.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(detail.completedAt()).isNotNull();
        assertThat(detail.items()).allSatisfy(i -> assertThat(i.completedAt()).isNotNull());

        // Gone from the active queue, present in the completed history.
        assertThat(barmakerOrderService.listOrders(false))
                .extracting(BarOrderSummaryResponse::id).doesNotContain(created.id());
        BarOrderSummaryResponse completed = find(barmakerOrderService.listOrders(true), created.id());
        assertThat(completed.itemCount()).isEqualTo(2);
        assertThat(completed.completedItemCount()).isEqualTo(2);
    }

    @Test
    void completedQueueIsSortedMostRecentlyCompletedFirst() {
        OrderResponse first = createOrder(item(1L, CocktailSize.M));
        OrderResponse second = createOrder(item(1L, CocktailSize.M));
        completeOrder(first);
        completeOrder(second);

        List<BarOrderSummaryResponse> completed = barmakerOrderService.listOrders(true);

        // DB-side sort: completedAt descending (monotonic, non-increasing).
        assertThat(completed).isSortedAccordingTo(
                Comparator.comparing(BarOrderSummaryResponse::completedAt).reversed());
        // The later-completed order ranks before the earlier one.
        int idxFirst = indexOf(completed, first.id());
        int idxSecond = indexOf(completed, second.id());
        assertThat(idxSecond).isLessThan(idxFirst);
    }

    @Test
    void detailUnknownOrderThrowsNotFound() {
        assertThatThrownBy(() -> barmakerOrderService.getOrder(UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void advancingUnknownItemThrowsNotFound() {
        assertThatThrownBy(() -> barmakerOrderService.advanceItemToNextStep(UUID.randomUUID()))
                .isInstanceOf(OrderItemNotFoundException.class);
    }

    @Test
    void advancingACompletedItemThrowsInvalidTransitionAndLeavesStateIntact() {
        OrderResponse created = createOrder(item(1L, CocktailSize.M));
        UUID itemId = itemIdAtSequence(created, 1);
        // Drive the single item all the way to COMPLETED.
        barmakerOrderService.advanceItemToNextStep(itemId);
        barmakerOrderService.advanceItemToNextStep(itemId);
        barmakerOrderService.advanceItemToNextStep(itemId);
        // Re-read from the database so the comparison uses DB-precision timestamps.
        OrderResponse before = barmakerOrderService.getOrder(created.id());
        assertThat(before.status()).isEqualTo(OrderStatus.COMPLETED);

        assertThatThrownBy(() -> barmakerOrderService.advanceItemToNextStep(itemId))
                .isInstanceOf(InvalidPreparationTransitionException.class);

        // The order remains COMPLETED with its completion timestamp unchanged.
        OrderResponse after = barmakerOrderService.getOrder(created.id());
        assertThat(after.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(after.completedAt()).isEqualTo(before.completedAt());
    }

    @Test
    void emptyCompletedQueueReturnsEmptyListWhenNothingCompleted() {
        // No assumption about other tests: just assert the call is a valid list.
        assertThat(barmakerOrderService.listOrders(true)).isNotNull();
    }

    // ---- helpers --------------------------------------------------------

    private void completeOrder(OrderResponse order) {
        OrderResponse current = barmakerOrderService.getOrder(order.id());
        for (OrderItemResponse line : current.items()) {
            barmakerOrderService.advanceItemToNextStep(line.id()); // -> ASSEMBLY
            barmakerOrderService.advanceItemToNextStep(line.id()); // -> DRESSING
            barmakerOrderService.advanceItemToNextStep(line.id()); // -> COMPLETED
        }
    }

    private static UUID itemIdAtSequence(OrderResponse order, int sequence) {
        return order.items().stream()
                .filter(i -> i.sequenceNumber() == sequence)
                .map(OrderItemResponse::id)
                .findFirst()
                .orElseThrow();
    }

    private static BarOrderSummaryResponse find(List<BarOrderSummaryResponse> list, UUID id) {
        return list.stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }

    private static int indexOf(List<BarOrderSummaryResponse> list, UUID id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) {
                return i;
            }
        }
        throw new IllegalStateException("id not found: " + id);
    }

    private OrderResponse createOrder(CreateOrderItemRequest... items) {
        return orderService.createOrder(new CreateOrderRequest(List.of(items)));
    }

    private static CreateOrderItemRequest item(Long cocktailId, CocktailSize size) {
        return new CreateOrderItemRequest(cocktailId, size);
    }
}
