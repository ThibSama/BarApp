package com.lebarapp.mapper;

import com.lebarapp.dto.OrderItemResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Explicit, testable mapping from order entities to tracking DTOs. Items are
 * always returned sorted by {@code sequenceNumber}. Entities are never exposed
 * beyond this boundary.
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .sorted(Comparator.comparingInt(OrderItem::getSequenceNumber))
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getPublicCode(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getCompletedAt(),
                items);
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getSequenceNumber(),
                item.getCocktailNameSnapshot(),
                item.getSize(),
                item.getUnitPriceSnapshot(),
                item.getPreparationStatus(),
                item.getCompletedAt());
    }
}
