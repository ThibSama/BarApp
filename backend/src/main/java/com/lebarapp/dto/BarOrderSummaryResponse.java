package com.lebarapp.dto;

import com.lebarapp.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Compact order summary for the protected barmaker queue
 * ({@code GET /api/bar/orders}). It deliberately omits the per-item detail and
 * instead exposes aggregate progress counters ({@code itemCount},
 * {@code completedItemCount}) so the dashboard can render a queue without
 * fetching every line. It is built directly by a JPQL aggregate query, so no JPA
 * entity is ever serialized and no per-item N+1 query is issued.
 */
public record BarOrderSummaryResponse(
        UUID id,
        String publicCode,
        OrderStatus status,
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        long itemCount,
        long completedItemCount) {
}
