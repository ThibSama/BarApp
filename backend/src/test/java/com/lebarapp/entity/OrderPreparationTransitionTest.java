package com.lebarapp.entity;

import com.lebarapp.enums.PaymentMethod;

import com.lebarapp.enums.CocktailSize;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.enums.PreparationStatus;
import com.lebarapp.exception.InvalidPreparationTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the preparation domain methods on {@link OrderItem} and the
 * aggregate status recomputation on {@link CustomerOrder}. No persistence is
 * involved: these verify the invariants the database constraints also enforce.
 */
class OrderPreparationTransitionTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);

    // ---- OrderItem transitions ------------------------------------------

    @Test
    void newItemStartsAtIngredientsWithNoCompletion() {
        OrderItem item = newItem(1);
        assertThat(item.getPreparationStatus()).isEqualTo(PreparationStatus.PREPARATION_INGREDIENTS);
        assertThat(item.getCompletedAt()).isNull();
    }

    @Test
    void advancesThroughEachStepInSequence() {
        OrderItem item = newItem(1);

        item.advanceToNextStep(NOW);
        assertThat(item.getPreparationStatus()).isEqualTo(PreparationStatus.ASSEMBLY);
        assertThat(item.getCompletedAt()).isNull();

        item.advanceToNextStep(NOW);
        assertThat(item.getPreparationStatus()).isEqualTo(PreparationStatus.DRESSING);
        assertThat(item.getCompletedAt()).isNull();

        item.advanceToNextStep(NOW);
        assertThat(item.getPreparationStatus()).isEqualTo(PreparationStatus.COMPLETED);
        assertThat(item.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void advancingACompletedItemIsRejected() {
        OrderItem item = newItem(1);
        item.advanceToNextStep(NOW);
        item.advanceToNextStep(NOW);
        item.advanceToNextStep(NOW); // now COMPLETED

        assertThatThrownBy(() -> item.advanceToNextStep(NOW))
                .isInstanceOf(InvalidPreparationTransitionException.class);
        // State is unchanged after the rejected transition.
        assertThat(item.getPreparationStatus()).isEqualTo(PreparationStatus.COMPLETED);
        assertThat(item.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void completedAtIsOnlySetForCompletedState() {
        OrderItem item = newItem(1);
        item.advanceToNextStep(NOW); // ASSEMBLY
        assertThat(item.getCompletedAt()).isNull();
        item.advanceToNextStep(NOW); // DRESSING
        assertThat(item.getCompletedAt()).isNull();
        item.advanceToNextStep(NOW); // COMPLETED
        assertThat(item.getCompletedAt()).isNotNull();
    }

    // ---- CustomerOrder aggregate status ---------------------------------

    @Test
    void brandNewOrderRemainsOrdered() {
        CustomerOrder order = orderWithItems(2);
        order.refreshStatus(NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(order.getCompletedAt()).isNull();
    }

    @Test
    void firstProgressionMovesOrderToInProgress() {
        CustomerOrder order = orderWithItems(2);
        order.getItems().get(0).advanceToNextStep(NOW); // one item -> ASSEMBLY

        order.refreshStatus(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(order.getCompletedAt()).isNull();
    }

    @Test
    void orderStaysInProgressWhileAnyItemUnfinished() {
        CustomerOrder order = orderWithItems(2);
        OrderItem first = order.getItems().get(0);
        first.advanceToNextStep(NOW);
        first.advanceToNextStep(NOW);
        first.advanceToNextStep(NOW); // first COMPLETED, second still at ingredients

        order.refreshStatus(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(order.getCompletedAt()).isNull();
    }

    @Test
    void orderBecomesCompletedWhenAllItemsFinish() {
        CustomerOrder order = orderWithItems(2);
        for (OrderItem item : order.getItems()) {
            item.advanceToNextStep(NOW);
            item.advanceToNextStep(NOW);
            item.advanceToNextStep(NOW);
        }

        order.refreshStatus(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void completedOrderHasItsTimestampPreservedOnRefresh() {
        CustomerOrder order = orderWithItems(1);
        OrderItem item = order.getItems().get(0);
        item.advanceToNextStep(NOW);
        item.advanceToNextStep(NOW);
        item.advanceToNextStep(NOW);
        order.refreshStatus(NOW);
        OffsetDateTime firstCompletion = order.getCompletedAt();

        // A later refresh must not move the completion timestamp.
        order.refreshStatus(NOW.plusHours(1));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isEqualTo(firstCompletion);
    }

    // ---- helpers --------------------------------------------------------

    private static OrderItem newItem(int sequence) {
        CustomerOrder order = new CustomerOrder(UUID.randomUUID(), "ABC234", new BigDecimal("10.50"), 12, PaymentMethod.CARD_IN_APP);
        return new OrderItem(UUID.randomUUID(), order, null, "Mojito",
                CocktailSize.M, new BigDecimal("10.50"), sequence);
    }

    private static CustomerOrder orderWithItems(int count) {
        CustomerOrder order = new CustomerOrder(UUID.randomUUID(), "ABC234", new BigDecimal("10.50"), 12, PaymentMethod.CARD_IN_APP);
        for (int i = 1; i <= count; i++) {
            order.addItem(new OrderItem(UUID.randomUUID(), order, null, "Mojito",
                    CocktailSize.M, new BigDecimal("10.50"), i));
        }
        return order;
    }
}
