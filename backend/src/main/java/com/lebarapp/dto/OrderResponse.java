package com.lebarapp.dto;

import com.lebarapp.enums.OrderStatus;
import com.lebarapp.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Stable tracking representation of an order, returned by {@code POST /api/orders}
 * and {@code GET /api/orders/{orderId}}. The {@code id} (UUID) is the secure,
 * non-predictable tracking reference; {@code publicCode} is for human display
 * only. {@code tableNumber} and {@code paymentMethod} are echoed back for the
 * confirmation/tracking screens. No JPA entity, internal timestamp metadata or
 * catalog id is exposed.
 */
public record OrderResponse(
        UUID id,
        String publicCode,
        OrderStatus status,
        BigDecimal totalAmount,
        int tableNumber,
        PaymentMethod paymentMethod,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        List<OrderItemResponse> items) {
}
