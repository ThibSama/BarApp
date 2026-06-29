package com.lebarapp.dto;

import com.lebarapp.enums.CocktailSize;
import com.lebarapp.enums.PreparationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One tracked drink within an order. {@code cocktailName} and {@code unitPrice}
 * are historical snapshots frozen at order time and never change afterwards.
 */
public record OrderItemResponse(
        UUID id,
        int sequenceNumber,
        String cocktailName,
        CocktailSize size,
        BigDecimal unitPrice,
        PreparationStatus preparationStatus,
        OffsetDateTime completedAt) {
}
