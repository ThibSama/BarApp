package com.lebarapp.security;

import com.lebarapp.config.JwtConfig;
import com.lebarapp.config.SecurityConfig;
import com.lebarapp.config.SecurityProperties;
import com.lebarapp.controller.BarmakerOrderController;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.enums.OrderStatus;
import com.lebarapp.repository.AppUserRepository;
import com.lebarapp.service.BarmakerOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route-authorization tests for the three real barmaker endpoints, exercised
 * through the production {@link SecurityConfig} filter chain. Verifies that each
 * route is unreachable without authentication (401), forbidden for an
 * insufficient role (403), and permitted for {@code ROLE_BARMAKER}. The JWT
 * converter is bypassed via {@code @WithMockUser}; signed-token validation is
 * covered by {@link SecurityIT}.
 */
@WebMvcTest(BarmakerOrderController.class)
@Import({SecurityConfig.class, JwtConfig.class,
        JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        ActiveUserJwtAuthenticationConverter.class})
@EnableConfigurationProperties(SecurityProperties.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-le-barapp-integration-tests-256bits",
        "app.jwt.issuer=le-barapp-test",
        "app.cors-allowed-origins=http://localhost:5173"
})
class BarmakerOrderRouteSecurityTest {

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

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    private OrderResponse stubOrder() {
        return new OrderResponse(ORDER_ID, "ABC234", OrderStatus.IN_PROGRESS,
                new BigDecimal("10.50"), OffsetDateTime.now(ZoneOffset.UTC), null, List.of());
    }

    // ---- 401 (no authentication) ----------------------------------------

    @Test
    void listWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/bar/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detailWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/api/bar/orders/" + ORDER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nextStepWithoutAuthReturns401() throws Exception {
        mockMvc.perform(patch("/api/bar/order-items/" + ITEM_ID + "/next-step"))
                .andExpect(status().isUnauthorized());
    }

    // ---- 403 (insufficient role) ----------------------------------------

    @Test
    @WithMockUser(roles = "VIEWER")
    void listWithInsufficientRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/bar/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void detailWithInsufficientRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/bar/orders/" + ORDER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void nextStepWithInsufficientRoleReturns403() throws Exception {
        mockMvc.perform(patch("/api/bar/order-items/" + ITEM_ID + "/next-step"))
                .andExpect(status().isForbidden());
    }

    // ---- 200 (ROLE_BARMAKER) --------------------------------------------

    @Test
    @WithMockUser(roles = "BARMAKER")
    void listAsBarmakerSucceeds() throws Exception {
        when(barmakerOrderService.listOrders(anyBoolean())).thenReturn(List.of());
        mockMvc.perform(get("/api/bar/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BARMAKER")
    void detailAsBarmakerSucceeds() throws Exception {
        when(barmakerOrderService.getOrder(any())).thenReturn(stubOrder());
        mockMvc.perform(get("/api/bar/orders/" + ORDER_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BARMAKER")
    void nextStepAsBarmakerSucceeds() throws Exception {
        when(barmakerOrderService.advanceItemToNextStep(any())).thenReturn(stubOrder());
        mockMvc.perform(patch("/api/bar/order-items/" + ITEM_ID + "/next-step"))
                .andExpect(status().isOk());
    }
}
