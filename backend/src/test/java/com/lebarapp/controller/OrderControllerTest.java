package com.lebarapp.controller;

import com.lebarapp.enums.PaymentMethod;

import com.lebarapp.dto.OrderResponse;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.exception.CocktailNotFoundException;
import com.lebarapp.exception.OrderNotFoundException;
import com.lebarapp.exception.PublicCodeGenerationException;
import com.lebarapp.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lebarapp.repository.AppUserRepository;

/**
 * Web-layer contract tests for {@link OrderController}: success shape + Location
 * header, request validation (400s), malformed identifiers and not-found (404).
 * The service is mocked to keep the focus on the HTTP contract and the
 * centralized error format.
 */
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-le-barapp-integration-tests-256bits",
        "app.jwt.issuer=le-barapp-test",
        "app.cors-allowed-origins=http://localhost:5173"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private com.lebarapp.security.BarmakerUserDetailsService barmakerUserDetailsService;

    @Test
    void validOrderReturns201WithLocationAndBody() throws Exception {
        UUID id = UUID.fromString("7fdbd20d-9e3a-4b7a-b807-82765d60432f");
        OrderResponse response = new OrderResponse(id, "ABC234", OrderStatus.ORDERED,
                new BigDecimal("10.50"), 12, PaymentMethod.CARD_IN_APP, OffsetDateTime.now(ZoneOffset.UTC), null, List.of());
        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"tableNumber\":12,\"paymentMethod\":\"CARD_IN_APP\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/orders/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.publicCode").value("ABC234"))
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.tableNumber").value(12))
                .andExpect(jsonPath("$.paymentMethod").value("CARD_IN_APP"));
    }

    @Test
    void emptyItemListReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingItemListReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void moreThanFiftyItemsReturns400() throws Exception {
        String items = IntStream.range(0, 51)
                .mapToObj(i -> "{\"cocktailId\":1,\"size\":\"M\"}")
                .collect(Collectors.joining(","));
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[" + items + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void nullItemReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void negativeCocktailIdReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":-1,\"size\":\"M\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidSizeEnumReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"XL\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":["))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void missingCocktailDuringCreationReturns404() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new CocktailNotFoundException(99L));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":99,\"size\":\"M\"}],\"tableNumber\":12,\"paymentMethod\":\"CARD_IN_APP\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COCKTAIL_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void malformedUuidInTrackingPathReturns400() throws Exception {
        mockMvc.perform(get("/api/orders/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDENTIFIER"));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(id)).thenThrow(new OrderNotFoundException());

        mockMvc.perform(get("/api/orders/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void publicCodeGenerationFailureReturns500() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new PublicCodeGenerationException());

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"tableNumber\":12,\"paymentMethod\":\"CARD_IN_APP\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("PUBLIC_CODE_GENERATION_FAILED"));
    }

    @Test
    void unexpectedErrorReturnsGeneric500WithoutLeakingDetails() throws Exception {
        when(orderService.createOrder(any()))
                .thenThrow(new RuntimeException("jdbc url=secret password=hunter2"));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"tableNumber\":12,\"paymentMethod\":\"CARD_IN_APP\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                // The internal exception message must never reach the client.
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("hunter2"))));
    }

    @Test
    void missingTableNumberReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"paymentMethod\":\"CARD_IN_APP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void tableNumberOutOfRangeReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"tableNumber\":1000,\"paymentMethod\":\"CARD_IN_APP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void zeroTableNumberReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"tableNumber\":0,\"paymentMethod\":\"CARD_IN_APP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingPaymentMethodReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"tableNumber\":12}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidPaymentMethodEnumReturns400() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"cocktailId\":1,\"size\":\"M\"}],\"tableNumber\":12,\"paymentMethod\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
