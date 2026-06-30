package com.lebarapp.controller;

import com.lebarapp.dto.BarOrderSummaryResponse;
import com.lebarapp.dto.OrderItemResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.enums.PreparationStatus;
import com.lebarapp.exception.InvalidPreparationTransitionException;
import com.lebarapp.exception.OrderItemNotFoundException;
import com.lebarapp.exception.OrderNotFoundException;
import com.lebarapp.repository.AppUserRepository;
import com.lebarapp.security.BarmakerUserDetailsService;
import com.lebarapp.service.BarmakerOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer contract tests for {@link BarmakerOrderController}. Security filters
 * are disabled here ({@code addFilters = false}) so the focus stays on the HTTP
 * contract, JSON shape and centralized error mapping; route authorization is
 * covered separately by {@link com.lebarapp.security.BarmakerOrderRouteSecurityTest}.
 */
@WebMvcTest(BarmakerOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-le-barapp-integration-tests-256bits",
        "app.jwt.issuer=le-barapp-test",
        "app.cors-allowed-origins=http://localhost:5173"
})
class BarmakerOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BarmakerOrderService barmakerOrderService;
    @MockitoBean
    @SuppressWarnings("unused")
    private AppUserRepository appUserRepository;
    @MockitoBean
    @SuppressWarnings("unused")
    private BarmakerUserDetailsService barmakerUserDetailsService;

    @Test
    void listReturnsSummaryArray() throws Exception {
        BarOrderSummaryResponse summary = new BarOrderSummaryResponse(
                UUID.randomUUID(), "ABC234", OrderStatus.IN_PROGRESS,
                new BigDecimal("21.00"), OffsetDateTime.now(ZoneOffset.UTC), null, 2, 1);
        when(barmakerOrderService.listOrders(false)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/bar/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicCode").value("ABC234"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].itemCount").value(2))
                .andExpect(jsonPath("$[0].completedItemCount").value(1));
    }

    @Test
    void emptyQueueReturns200WithEmptyArray() throws Exception {
        when(barmakerOrderService.listOrders(false)).thenReturn(List.of());

        mockMvc.perform(get("/api/bar/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void completedQueryParameterIsForwarded() throws Exception {
        when(barmakerOrderService.listOrders(true)).thenReturn(List.of());

        mockMvc.perform(get("/api/bar/orders").param("completed", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void detailReturnsOrderResponse() throws Exception {
        UUID id = UUID.randomUUID();
        OrderResponse response = new OrderResponse(id, "ABC234", OrderStatus.ORDERED,
                new BigDecimal("10.50"), OffsetDateTime.now(ZoneOffset.UTC), null,
                List.of(new OrderItemResponse(UUID.randomUUID(), 1, "Mojito", CocktailSize.M,
                        new BigDecimal("10.50"), PreparationStatus.PREPARATION_INGREDIENTS, null)));
        when(barmakerOrderService.getOrder(id)).thenReturn(response);

        mockMvc.perform(get("/api/bar/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].preparationStatus").value("PREPARATION_INGREDIENTS"));
    }

    @Test
    void detailUnknownOrderReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(barmakerOrderService.getOrder(id)).thenThrow(new OrderNotFoundException());

        mockMvc.perform(get("/api/bar/orders/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void detailMalformedUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/bar/orders/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDENTIFIER"));
    }

    @Test
    void nextStepReturnsRefreshedOrder() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse(orderId, "ABC234", OrderStatus.IN_PROGRESS,
                new BigDecimal("10.50"), OffsetDateTime.now(ZoneOffset.UTC), null,
                List.of(new OrderItemResponse(itemId, 1, "Mojito", CocktailSize.M,
                        new BigDecimal("10.50"), PreparationStatus.ASSEMBLY, null)));
        when(barmakerOrderService.advanceItemToNextStep(itemId)).thenReturn(response);

        mockMvc.perform(patch("/api/bar/order-items/" + itemId + "/next-step"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.items[0].preparationStatus").value("ASSEMBLY"));
    }

    @Test
    void nextStepUnknownItemReturns404() throws Exception {
        when(barmakerOrderService.advanceItemToNextStep(any()))
                .thenThrow(new OrderItemNotFoundException());

        mockMvc.perform(patch("/api/bar/order-items/" + UUID.randomUUID() + "/next-step"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_ITEM_NOT_FOUND"));
    }

    @Test
    void nextStepOnCompletedItemReturns409() throws Exception {
        when(barmakerOrderService.advanceItemToNextStep(any()))
                .thenThrow(new InvalidPreparationTransitionException());

        mockMvc.perform(patch("/api/bar/order-items/" + UUID.randomUUID() + "/next-step"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PREPARATION_TRANSITION"));
    }

    @Test
    void nextStepMalformedUuidReturns400() throws Exception {
        mockMvc.perform(patch("/api/bar/order-items/not-a-uuid/next-step"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDENTIFIER"));
    }
}
