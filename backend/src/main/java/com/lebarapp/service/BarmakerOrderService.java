package com.lebarapp.service;

import com.lebarapp.dto.BarOrderSummaryResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.entity.OrderItem;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.exception.OrderItemNotFoundException;
import com.lebarapp.exception.OrderNotFoundException;
import com.lebarapp.mapper.OrderMapper;
import com.lebarapp.repository.CustomerOrderRepository;
import com.lebarapp.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Protected barmaker order-processing service. It exposes the work queue, the
 * full detail of one order, and the single-step preparation progression that
 * also keeps the parent order status in sync.
 *
 * <p>The progression runs in one transaction and acquires a pessimistic write
 * lock on the owning order, so concurrent barmaker actions on the same order
 * serialize and cannot lose updates. The lock scope is restricted to the single
 * affected aggregate; the rest of the queue is never locked.</p>
 */
@Service
public class BarmakerOrderService {

    /** Active queue = not yet finished. */
    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.ORDERED, OrderStatus.IN_PROGRESS);
    private static final List<OrderStatus> COMPLETED_STATUSES =
            List.of(OrderStatus.COMPLETED);

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;

    public BarmakerOrderService(CustomerOrderRepository customerOrderRepository,
                                OrderItemRepository orderItemRepository,
                                OrderMapper orderMapper) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * Lists orders for the queue. {@code completed=false} (the default) returns
     * the active orders ({@code ORDERED} + {@code IN_PROGRESS}) oldest-first;
     * {@code completed=true} returns the {@code COMPLETED} history, most recently
     * completed first. An empty result is a valid, empty list.
     */
    @Transactional(readOnly = true)
    public List<BarOrderSummaryResponse> listOrders(boolean completed) {
        return completed
                ? customerOrderRepository.findCompletedSummaries(COMPLETED_STATUSES)
                : customerOrderRepository.findActiveSummaries(ACTIVE_STATUSES);
    }

    /** Full detail of one order (items sorted by sequence number). 404 if unknown. */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        CustomerOrder order = customerOrderRepository.findWithItemsById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        return orderMapper.toResponse(order);
    }

    /**
     * Advances one order item by exactly one preparation step and recomputes the
     * parent order status, atomically and under an aggregate lock. Returns the
     * complete, refreshed parent order so the caller can immediately render the
     * updated detail.
     *
     * @throws OrderItemNotFoundException if the item does not exist (404)
     * @throws com.lebarapp.exception.InvalidPreparationTransitionException if the
     *         item is already completed (409)
     */
    @Transactional
    public OrderResponse advanceItemToNextStep(UUID itemId) {
        // Resolve the owning order first, then lock that single row, so the
        // pessimistic lock is acquired through a real SELECT ... FOR UPDATE.
        UUID orderId = orderItemRepository.findOrderIdById(itemId)
                .orElseThrow(OrderItemNotFoundException::new);
        CustomerOrder order = customerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(OrderNotFoundException::new);

        OrderItem item = order.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(OrderItemNotFoundException::new);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        item.advanceToNextStep(now);   // 409 if already completed
        order.refreshStatus(now);

        // Flush within the locked transaction so DB constraints are checked and
        // the lock is held until commit.
        customerOrderRepository.flush();
        return orderMapper.toResponse(order);
    }
}
