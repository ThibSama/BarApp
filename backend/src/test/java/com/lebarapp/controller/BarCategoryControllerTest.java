package com.lebarapp.controller;

import com.lebarapp.dto.CategoryResponse;
import com.lebarapp.exception.CategoryAlreadyExistsException;
import com.lebarapp.exception.CategoryNotFoundException;
import com.lebarapp.repository.AppUserRepository;
import com.lebarapp.security.BarmakerUserDetailsService;
import com.lebarapp.service.CategoryAdminService;
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
 * Web-layer contract tests for {@link BarCategoryController}. Security filters
 * are disabled here ({@code addFilters = false}) so the focus stays on the HTTP
 * contract, JSON shape and centralized error mapping; route authorization is
 * covered separately by {@link com.lebarapp.security.CatalogRouteSecurityTest}.
 */
@WebMvcTest(BarCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-le-barapp-integration-tests-256bits",
        "app.jwt.issuer=le-barapp-test",
        "app.cors-allowed-origins=http://localhost:5173"
})
class BarCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryAdminService categoryService;
    @MockitoBean
    @SuppressWarnings("unused")
    private AppUserRepository appUserRepository;
    @MockitoBean
    @SuppressWarnings("unused")
    private BarmakerUserDetailsService barmakerUserDetailsService;

    @Test
    void listReturnsCategories() throws Exception {
        when(categoryService.list()).thenReturn(List.of(
                new CategoryResponse(1L, "Classiques", "desc", 1, true),
                new CategoryResponse(4L, "Promotions expirées", null, 99, false)));

        mockMvc.perform(get("/api/bar/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Classiques"))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(categoryService.create(any())).thenReturn(
                new CategoryResponse(5L, "Signatures", "Nos créations", 4, true));

        mockMvc.perform(post("/api/bar/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Signatures\",\"description\":\"Nos créations\",\"displayOrder\":4}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/bar/categories/5")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/bar/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"displayOrder\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createNegativeDisplayOrderReturns400() throws Exception {
        mockMvc.perform(post("/api/bar/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"displayOrder\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createDuplicateNameReturns409() throws Exception {
        when(categoryService.create(any())).thenThrow(new CategoryAlreadyExistsException("Classiques"));

        mockMvc.perform(post("/api/bar/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Classiques\",\"displayOrder\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void updateReturnsUpdatedCategory() throws Exception {
        when(categoryService.update(eq(1L), any())).thenReturn(
                new CategoryResponse(1L, "Classiques", "maj", 2, true));

        mockMvc.perform(put("/api/bar/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Classiques\",\"description\":\"maj\",\"displayOrder\":2,\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayOrder").value(2));
    }

    @Test
    void updateUnknownReturns404() throws Exception {
        when(categoryService.update(eq(999L), any())).thenThrow(new CategoryNotFoundException(999L));

        mockMvc.perform(put("/api/bar/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"displayOrder\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc.perform(delete("/api/bar/categories/1"))
                .andExpect(status().isNoContent());
        verify(categoryService).deactivate(1L);
    }

    @Test
    void deactivateUnknownReturns404() throws Exception {
        doThrow(new CategoryNotFoundException(999L)).when(categoryService).deactivate(999L);

        mockMvc.perform(delete("/api/bar/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }
}
