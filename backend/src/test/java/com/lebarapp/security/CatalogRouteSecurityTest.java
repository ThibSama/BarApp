package com.lebarapp.security;

import com.lebarapp.config.JwtConfig;
import com.lebarapp.config.SecurityConfig;
import com.lebarapp.config.SecurityProperties;
import com.lebarapp.controller.BarCategoryController;
import com.lebarapp.controller.BarCocktailController;
import com.lebarapp.controller.BarIngredientController;
import com.lebarapp.repository.AppUserRepository;
import com.lebarapp.service.CategoryAdminService;
import com.lebarapp.service.CocktailAdminService;
import com.lebarapp.service.IngredientAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route-authorization tests for the protected catalogue-management endpoints,
 * exercised through the production {@link SecurityConfig} filter chain. Verifies
 * each route is unreachable anonymously (401), forbidden for an insufficient
 * role (403) and permitted for {@code ROLE_BARMAKER}. The JWT converter is
 * bypassed via {@code @WithMockUser}; signed-token validation is covered by
 * {@link SecurityIT}.
 */
@WebMvcTest({BarCategoryController.class, BarCocktailController.class, BarIngredientController.class})
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
class CatalogRouteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryAdminService categoryService;
    @MockitoBean
    private CocktailAdminService cocktailService;
    @MockitoBean
    private IngredientAdminService ingredientService;
    @MockitoBean
    @SuppressWarnings("unused")
    private AppUserRepository appUserRepository;
    @MockitoBean
    @SuppressWarnings("unused")
    private BarmakerUserDetailsService barmakerUserDetailsService;

    private static final String CATEGORY_BODY = "{\"name\":\"X\",\"displayOrder\":1}";

    // ---- 401 (no authentication) ----------------------------------------

    @Test
    void anonymousCategoryRoutesReturn401() throws Exception {
        mockMvc.perform(get("/api/bar/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bar/categories")
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/bar/categories/1")
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/bar/categories/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousCocktailRoutesReturn401() throws Exception {
        mockMvc.perform(get("/api/bar/cocktails")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bar/cocktails/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bar/cocktails")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/bar/cocktails/1")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/bar/cocktails/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousIngredientRoutesReturn401() throws Exception {
        mockMvc.perform(get("/api/bar/ingredients")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bar/ingredients/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bar/ingredients")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/bar/ingredients/1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/bar/ingredients/1")).andExpect(status().isUnauthorized());
    }

    // ---- 403 (insufficient role) ----------------------------------------

    @Test
    @WithMockUser(roles = "VIEWER")
    void insufficientRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/bar/categories")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bar/cocktails")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bar/ingredients")).andExpect(status().isForbidden());
    }

    // ---- 200 (ROLE_BARMAKER) --------------------------------------------

    @Test
    @WithMockUser(roles = "BARMAKER")
    void barmakerCanListCategories() throws Exception {
        when(categoryService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/bar/categories")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BARMAKER")
    void barmakerCanListCocktails() throws Exception {
        when(cocktailService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/bar/cocktails")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BARMAKER")
    void barmakerCanListIngredients() throws Exception {
        when(ingredientService.list()).thenReturn(List.of());
        mockMvc.perform(get("/api/bar/ingredients")).andExpect(status().isOk());
    }
}
