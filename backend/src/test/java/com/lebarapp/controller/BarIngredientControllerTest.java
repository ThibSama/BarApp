package com.lebarapp.controller;

import com.lebarapp.dto.IngredientResponse;
import com.lebarapp.exception.IngredientAlreadyExistsException;
import com.lebarapp.exception.IngredientNotFoundException;
import com.lebarapp.repository.AppUserRepository;
import com.lebarapp.security.BarmakerUserDetailsService;
import com.lebarapp.service.IngredientAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer contract tests for {@link BarIngredientController} (filters disabled).
 * Route authorization is covered by
 * {@link com.lebarapp.security.CatalogRouteSecurityTest}.
 */
@WebMvcTest(BarIngredientController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-le-barapp-integration-tests-256bits",
        "app.jwt.issuer=le-barapp-test",
        "app.cors-allowed-origins=http://localhost:5173"
})
class BarIngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientAdminService ingredientService;
    @MockitoBean
    @SuppressWarnings("unused")
    private AppUserRepository appUserRepository;
    @MockitoBean
    @SuppressWarnings("unused")
    private BarmakerUserDetailsService barmakerUserDetailsService;

    @Test
    void listReturnsIngredients() throws Exception {
        when(ingredientService.list()).thenReturn(List.of(
                new IngredientResponse(1L, "Rhum blanc", true),
                new IngredientResponse(13L, "Colorant de test", false)));

        mockMvc.perform(get("/api/bar/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rhum blanc"))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void detailReturnsIngredient() throws Exception {
        when(ingredientService.getById(1L)).thenReturn(new IngredientResponse(1L, "Rhum blanc", true));
        mockMvc.perform(get("/api/bar/ingredients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void detailUnknownReturns404() throws Exception {
        when(ingredientService.getById(999L)).thenThrow(new IngredientNotFoundException(999L));
        mockMvc.perform(get("/api/bar/ingredients/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(ingredientService.create(any())).thenReturn(new IngredientResponse(5L, "Basilic", true));
        mockMvc.perform(post("/api/bar/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Basilic\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/bar/ingredients/5")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/bar/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createTooLongNameReturns400() throws Exception {
        String longName = "x".repeat(121);
        mockMvc.perform(post("/api/bar/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createDuplicateNameReturns409() throws Exception {
        when(ingredientService.create(any())).thenThrow(new IngredientAlreadyExistsException("Menthe"));
        mockMvc.perform(post("/api/bar/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Menthe\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INGREDIENT_ALREADY_EXISTS"));
    }

    @Test
    void updateReturnsUpdatedIngredient() throws Exception {
        when(ingredientService.update(eq(1L), any()))
                .thenReturn(new IngredientResponse(1L, "Rhum ambré", true));
        mockMvc.perform(put("/api/bar/ingredients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rhum ambré\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rhum ambré"));
    }

    @Test
    void updateUnknownReturns404() throws Exception {
        when(ingredientService.update(eq(999L), any())).thenThrow(new IngredientNotFoundException(999L));
        mockMvc.perform(put("/api/bar/ingredients/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
    }

    @Test
    void updateConflictReturns409() throws Exception {
        when(ingredientService.update(eq(1L), any()))
                .thenThrow(new IngredientAlreadyExistsException("Menthe"));
        mockMvc.perform(put("/api/bar/ingredients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Menthe\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INGREDIENT_ALREADY_EXISTS"));
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc.perform(delete("/api/bar/ingredients/1"))
                .andExpect(status().isNoContent());
        verify(ingredientService).deactivate(1L);
    }

    @Test
    void deactivateUnknownReturns404() throws Exception {
        doThrow(new IngredientNotFoundException(999L)).when(ingredientService).deactivate(999L);
        mockMvc.perform(delete("/api/bar/ingredients/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
    }
}
