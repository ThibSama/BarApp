package com.lebarapp.service;

import com.lebarapp.enums.PaymentMethod;

import com.lebarapp.dto.BarOrderSummaryResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.entity.OrderItem;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.enums.PreparationStatus;
import com.lebarapp.exception.InvalidPreparationTransitionException;
import com.lebarapp.exception.OrderItemNotFoundException;
import com.lebarapp.exception.OrderNotFoundException;
import com.lebarapp.mapper.OrderMapper;
import com.lebarapp.repository.CustomerOrderRepository;
import com.lebarapp.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Deterministic unit tests for {@link BarmakerOrderService}. Repositories are
 * mocked but the real {@link OrderMapper} is used so the returned DTO can be
 * asserted directly. The aggregate lock path and status recomputation are
 * exercised without a database.
 */
@ExtendWith(MockitoExtension.class)
class BarmakerOrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;

    private BarmakerOrderService service;

    @BeforeEach
    void setUp() {
        service = new BarmakerOrderService(customerOrderRepository, orderItemRepository, new OrderMapper());
    }

    // ---- listing --------------------------------------------------------

    @Test
    void listActiveOrdersQueriesActiveStatuses() {
        BarOrderSummaryResponse summary = summary(OrderStatus.ORDERED);
        when(customerOrderRepository.findActiveSummaries(
                eq(List.of(OrderStatus.ORDERED, OrderStatus.IN_PROGRESS))))
                .thenReturn(List.of(summary));

        List<BarOrderSummaryResponse> result = service.listOrders(false);

        assertThat(result).containsExactly(summary);
        verify(customerOrderRepository, never()).findCompletedSummaries(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listCompletedOrdersQueriesCompletedStatus() {
        BarOrderSummaryResponse summary = summary(OrderStatus.COMPLETED);
        when(customerOrderRepository.findCompletedSummaries(eq(List.of(OrderStatus.COMPLETED))))
                .thenReturn(List.of(summary));

        List<BarOrderSummaryResponse> result = service.listOrders(true);

        assertThat(result).containsExactly(summary);
        verify(customerOrderRepository, never()).findActiveSummaries(org.mockito.ArgumentMatchers.any());
    }

    // ---- detail ---------------------------------------------------------

    @Test
    void getOrderReturnsDetailWhenFound() {
        CustomerOrder order = orderWithItems(2);
        when(customerOrderRepository.findWithItemsById(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = service.getOrder(order.getId());

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void getOrderThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(customerOrderRepository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(id)).isInstanceOf(OrderNotFoundException.class);
    }

    // ---- next-step progression -----------------------------------------

    @Test
    void advanceMovesOneStepAndFlushesUnderLock() {
        CustomerOrder order = orderWithItems(2);
        OrderItem target = order.getItems().get(0);
        when(orderItemRepository.findOrderIdById(target.getId())).thenReturn(Optional.of(order.getId()));
        when(customerOrderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = service.advanceItemToNextStep(target.getId());

        assertThat(target.getPreparationStatus()).isEqualTo(PreparationStatus.ASSEMBLY);
        // One item started -> the parent order is now IN_PROGRESS.
        assertThat(response.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        verify(customerOrderRepository).findByIdForUpdate(order.getId());
        verify(customerOrderRepository).flush();
    }

    @Test
    void advanceCompletesOrderWhenLastItemFinishes() {
        CustomerOrder order = orderWithItems(1);
        OrderItem only = order.getItems().get(0);
        only.advanceToNextStep(now()); // ASSEMBLY
        only.advanceToNextStep(now()); // DRESSING
        when(orderItemRepository.findOrderIdById(only.getId())).thenReturn(Optional.of(order.getId()));
        when(customerOrderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        OrderResponse response = service.advanceItemToNextStep(only.getId());

        assertThat(only.getPreparationStatus()).isEqualTo(PreparationStatus.COMPLETED);
        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void advanceUnknownItemThrowsNotFoundAndNeverLocks() {
        UUID itemId = UUID.randomUUID();
        when(orderItemRepository.findOrderIdById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.advanceItemToNextStep(itemId))
                .isInstanceOf(OrderItemNotFoundException.class);
        verify(customerOrderRepository, never()).findByIdForUpdate(org.mockito.ArgumentMatchers.any());
        verify(customerOrderRepository, never()).flush();
    }

    @Test
    void advanceCompletedItemIsRejectedWithoutFlush() {
        CustomerOrder order = orderWithItems(1);
        OrderItem only = order.getItems().get(0);
        only.advanceToNextStep(now());
        only.advanceToNextStep(now());
        only.advanceToNextStep(now()); // COMPLETED
        when(orderItemRepository.findOrderIdById(only.getId())).thenReturn(Optional.of(order.getId()));
        when(customerOrderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.advanceItemToNextStep(only.getId()))
                .isInstanceOf(InvalidPreparationTransitionException.class);
        // No partial mutation persisted: the failing transition never reaches flush.
        verify(customerOrderRepository, never()).flush();
    }

    // ---- helpers --------------------------------------------------------

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static BarOrderSummaryResponse summary(OrderStatus status) {
        return new BarOrderSummaryResponse(UUID.randomUUID(), "ABC234", status,
                new BigDecimal("10.50"), 12, now(), null, 1, 0);
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
