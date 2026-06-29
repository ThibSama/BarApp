package com.lebarapp.security;

import com.lebarapp.config.JwtConfig;
import com.lebarapp.config.SecurityConfig;
import com.lebarapp.config.SecurityProperties;
import com.lebarapp.controller.BarTestController;
import com.lebarapp.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import com.lebarapp.config.SecurityProperties;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused authorization test for {@code /api/bar/**} using Spring Security test
 * support. This test bypasses the production JWT converter intentionally: its
 * purpose is to validate that the route authorization rule and the JSON
 * {@link JsonAccessDeniedHandler} return a proper 403 when the authenticated
 * principal lacks {@code ROLE_BARMAKER}.
 *
 * <p>Production authentication tests (login, JWT, disabled user) are covered by
 * {@link SecurityIT} using real signed JWTs and PostgreSQL users.</p>
 */
@WebMvcTest(BarTestController.class)
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
class BarRouteAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private AppUserRepository appUserRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private BarmakerUserDetailsService barmakerUserDetailsService;

    @Test
    @WithMockUser(roles = "VIEWER")
    void insufficientRoleReturns403AccessDenied() throws Exception {
        mockMvc.perform(get("/api/bar/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(roles = "BARMAKER")
    void barmakerRoleSucceedsOnBarRoute() throws Exception {
        mockMvc.perform(get("/api/bar/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
