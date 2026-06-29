package com.lebarapp.controller;

import com.lebarapp.dto.MenuCategoryDto;
import com.lebarapp.dto.MenuCocktailDto;
import com.lebarapp.dto.MenuIngredientDto;
import com.lebarapp.dto.MenuPriceDto;
import com.lebarapp.dto.MenuResponse;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.service.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lebarapp.repository.AppUserRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test of {@code GET /api/menu}: verifies HTTP 200, the JSON contract
 * shape (including exact decimal serialization), and the empty-catalog response.
 * The service is mocked so the test stays focused on the controller contract.
 */
@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-for-le-barapp-integration-tests-256bits",
        "app.jwt.issuer=le-barapp-test",
        "app.cors-allowed-origins=http://localhost:5173"
})
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private com.lebarapp.security.BarmakerUserDetailsService barmakerUserDetailsService;

    @Test
    void returnsMenuJsonShape() throws Exception {
        MenuResponse response = new MenuResponse(List.of(
                new MenuCategoryDto(1L, "Classiques", 1, List.of(
                        new MenuCocktailDto(1L, "Mojito",
                                "Rhum blanc, citron vert, menthe et eau gazeuse",
                                null,
                                List.of(new MenuIngredientDto(1L, "Rhum blanc", "4 cl", 1)),
                                List.of(new MenuPriceDto(CocktailSize.S, new BigDecimal("8.50"))))))));
        when(menuService.getMenu()).thenReturn(response);

        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categories[0].id").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("Classiques"))
                .andExpect(jsonPath("$.categories[0].displayOrder").value(1))
                .andExpect(jsonPath("$.categories[0].cocktails[0].name").value("Mojito"))
                .andExpect(jsonPath("$.categories[0].cocktails[0].imageUrl").doesNotExist())
                .andExpect(jsonPath("$.categories[0].cocktails[0].ingredients[0].quantityLabel").value("4 cl"))
                .andExpect(jsonPath("$.categories[0].cocktails[0].prices[0].size").value("S"))
                // Exact decimal serialization (no float rounding).
                .andExpect(jsonPath("$.categories[0].cocktails[0].prices[0].price").value(8.50));
    }

    @Test
    void emptyCatalogReturnsEmptyCategoriesArray() throws Exception {
        when(menuService.getMenu()).thenReturn(new MenuResponse(List.of()));

        mockMvc.perform(get("/api/menu"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"categories\":[]}"));
    }
}
