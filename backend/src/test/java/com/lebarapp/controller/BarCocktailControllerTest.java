package com.lebarapp.controller;

import com.lebarapp.dto.CocktailIngredientResponse;
import com.lebarapp.dto.CocktailPriceResponse;
import com.lebarapp.dto.CocktailResponse;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.exception.CategoryNotFoundException;
import com.lebarapp.exception.CocktailAlreadyExistsException;
import com.lebarapp.exception.CocktailNotFoundException;
import com.lebarapp.exception.InactiveCategoryException;
import com.lebarapp.exception.InvalidCatalogRequestException;
import com.lebarapp.repository.AppUserRepository;
import com.lebarapp.security.BarmakerUserDetailsService;
import com.lebarapp.service.CocktailAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * Web-layer contract tests for {@link BarCocktailController} (filters disabled).
 * Route authorization is covered by
 * {@link com.lebarapp.security.CatalogRouteSecurityTest}.
 */
@WebMvcTest(BarCocktailController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-le-barapp-integration-tests-256bits",
        "app.jwt.issuer=le-barapp-test",
        "app.cors-allowed-origins=http://localhost:5173"
})
class BarCocktailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CocktailAdminService cocktailService;
    @MockitoBean
    @SuppressWarnings("unused")
    private AppUserRepository appUserRepository;
    @MockitoBean
    @SuppressWarnings("unused")
    private BarmakerUserDetailsService barmakerUserDetailsService;

    private static final String VALID_BODY = """
            {
              "categoryId": 1,
              "name": "Mojito",
              "description": "Cocktail frais à base de rhum.",
              "shortDescription": "Rhum, menthe et citron vert.",
              "imageUrl": "https://example.test/mojito.jpg",
              "ingredients": [
                {"name": "Rhum blanc", "quantityLabel": "5 cl", "displayOrder": 1},
                {"name": "Menthe", "quantityLabel": "8 feuilles", "displayOrder": 2}
              ],
              "prices": [
                {"size": "S", "price": 7.50},
                {"size": "M", "price": 9.00},
                {"size": "L", "price": 11.00}
              ]
            }
            """;

    private static CocktailResponse stub() {
        return new CocktailResponse(10L, 1L, "Classiques", "Mojito",
                "Cocktail frais à base de rhum.", "Rhum, menthe et citron vert.",
                "https://example.test/mojito.jpg", true,
                List.of(new CocktailIngredientResponse(1L, "Rhum blanc", "5 cl", 1)),
                List.of(new CocktailPriceResponse(CocktailSize.S, new BigDecimal("7.50"))));
    }

    @Test
    void listReturnsCocktails() throws Exception {
        when(cocktailService.list()).thenReturn(List.of(stub()));
        mockMvc.perform(get("/api/bar/cocktails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mojito"))
                .andExpect(jsonPath("$[0].categoryName").value("Classiques"));
    }

    @Test
    void detailReturnsCocktail() throws Exception {
        when(cocktailService.getById(10L)).thenReturn(stub());
        mockMvc.perform(get("/api/bar/cocktails/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.prices[0].size").value("S"));
    }

    @Test
    void detailUnknownReturns404() throws Exception {
        when(cocktailService.getById(999L)).thenThrow(new CocktailNotFoundException(999L));
        mockMvc.perform(get("/api/bar/cocktails/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COCKTAIL_NOT_FOUND"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(cocktailService.create(any())).thenReturn(stub());
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/bar/cocktails/10")))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createUnknownCategoryReturns404() throws Exception {
        when(cocktailService.create(any())).thenThrow(new CategoryNotFoundException(999L));
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void createUnderInactiveCategoryReturns409() throws Exception {
        when(cocktailService.create(any())).thenThrow(new InactiveCategoryException(4L));
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_INACTIVE"));
    }

    @Test
    void createDuplicateNameReturns409() throws Exception {
        when(cocktailService.create(any())).thenThrow(new CocktailAlreadyExistsException("Mojito"));
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COCKTAIL_ALREADY_EXISTS"));
    }

    @Test
    void createBlankNameReturns400() throws Exception {
        String body = VALID_BODY.replace("\"name\": \"Mojito\"", "\"name\": \"   \"");
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createEmptyIngredientsReturns400() throws Exception {
        String body = """
                {
                  "categoryId": 1, "name": "X", "description": "d",
                  "ingredients": [],
                  "prices": [
                    {"size": "S", "price": 7.50},
                    {"size": "M", "price": 9.00},
                    {"size": "L", "price": 11.00}
                  ]
                }
                """;
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createWithTwoPricesReturns400() throws Exception {
        String body = """
                {
                  "categoryId": 1, "name": "X", "description": "d",
                  "ingredients": [{"name": "Rhum", "displayOrder": 1}],
                  "prices": [
                    {"size": "S", "price": 7.50},
                    {"size": "M", "price": 9.00}
                  ]
                }
                """;
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createNegativePriceReturns400() throws Exception {
        String body = VALID_BODY.replace("\"price\": 7.50", "\"price\": -1.00");
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createDuplicateSizeReturns400() throws Exception {
        when(cocktailService.create(any()))
                .thenThrow(new InvalidCatalogRequestException("La taille S est fournie plusieurs fois."));
        // Body still passes bean validation (3 prices) but service rejects the duplicate size.
        String body = VALID_BODY.replace("{\"size\": \"L\", \"price\": 11.00}",
                "{\"size\": \"S\", \"price\": 11.00}");
        mockMvc.perform(post("/api/bar/cocktails")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATALOG_REQUEST"));
    }

    @Test
    void updateReturnsUpdatedCocktail() throws Exception {
        when(cocktailService.update(eq(10L), any())).thenReturn(stub());
        mockMvc.perform(put("/api/bar/cocktails/10")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void deactivateReturns204() throws Exception {
        mockMvc.perform(delete("/api/bar/cocktails/10"))
                .andExpect(status().isNoContent());
        verify(cocktailService).deactivate(10L);
    }
}
