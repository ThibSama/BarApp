package com.lebarapp.controller;

import com.lebarapp.dto.BarOrderSummaryResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.service.BarmakerOrderService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Protected barmaker order-processing API. Every route lives under
 * {@code /api/bar/**} and therefore requires an authenticated staff member
 * ({@code ROLE_BARMAKER} or {@code ROLE_MANAGER}, enforced by
 * the security filter chain). Entities are never exposed; only DTOs cross this
 * boundary.
 */
@RestController
@RequestMapping("/api/bar")
public class BarmakerOrderController {

    private final BarmakerOrderService barmakerOrderService;

    public BarmakerOrderController(BarmakerOrderService barmakerOrderService) {
        this.barmakerOrderService = barmakerOrderService;
    }

    /** Work queue. {@code completed} is optional and defaults to {@code false}. */
    @GetMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<BarOrderSummaryResponse> listOrders(
            @RequestParam(name = "completed", defaultValue = "false") boolean completed) {
        return barmakerOrderService.listOrders(completed);
    }

    /** Full detail of one order. 404 if unknown. */
    @GetMapping(value = "/orders/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponse getOrder(@PathVariable UUID orderId) {
        return barmakerOrderService.getOrder(orderId);
    }

    /**
     * Advances one order item by exactly one preparation step and returns the
     * complete, refreshed parent order. 404 if the item is unknown, 409 if it is
     * already completed.
     */
    @PatchMapping(value = "/order-items/{itemId}/next-step", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponse advanceItem(@PathVariable UUID itemId) {
        return barmakerOrderService.advanceItemToNextStep(itemId);
    }
}
