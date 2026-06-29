package com.lebarapp.repository;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.entity.AppUser;
import com.lebarapp.entity.Category;
import com.lebarapp.entity.Cocktail;
import com.lebarapp.entity.CocktailIngredient;
import com.lebarapp.entity.CocktailPrice;
import com.lebarapp.entity.Ingredient;
import com.lebarapp.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that every catalog entity (plus the out-of-scope {@link AppUser},
 * mapped ahead of the future authentication pass) maps correctly against the
 * real schema: audit timestamps are populated by the database and id-based
 * identity behaves as expected. These checks guard the persistence model the
 * order flow and future passes depend on.
 */
class CatalogEntityMappingIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private CocktailRepository cocktailRepository;
    @Autowired
    private CocktailIngredientRepository cocktailIngredientRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void categoryIsMappedWithAuditTimestampsAndIdentity() {
        Category category = categoryRepository.findById(1L).orElseThrow();
        assertThat(category.getName()).isEqualTo("Classiques");
        assertThat(category.getDisplayOrder()).isEqualTo(1);
        assertThat(category.isActive()).isTrue();
        assertThat(category.getCreatedAt()).isNotNull();
        assertThat(category.getUpdatedAt()).isNotNull();

        Category same = categoryRepository.findById(1L).orElseThrow();
        assertThat(category).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo("x");
    }

    @Test
    void ingredientIsMappedWithAuditTimestamps() {
        Ingredient ingredient = ingredientRepository.findById(1L).orElseThrow();
        assertThat(ingredient.getName()).isNotBlank();
        assertThat(ingredient.isActive()).isTrue();
        assertThat(ingredient.getCreatedAt()).isNotNull();
        assertThat(ingredient.getUpdatedAt()).isNotNull();
        assertThat(ingredient).isEqualTo(ingredientRepository.findById(1L).orElseThrow());
    }

    @Test
    void cocktailAndPriceAreMappedWithAuditTimestamps() {
        Cocktail cocktail = cocktailRepository.findWithPricesById(1L).orElseThrow();
        assertThat(cocktail.getName()).isEqualTo("Mojito");
        assertThat(cocktail.getDescription()).isNotBlank();
        assertThat(cocktail.getImageUrl()).isNull();
        assertThat(cocktail.isActive()).isTrue();
        assertThat(cocktail.getCreatedAt()).isNotNull();
        assertThat(cocktail.getUpdatedAt()).isNotNull();
        assertThat(cocktail).isEqualTo(cocktailRepository.findById(1L).orElseThrow())
                .isNotEqualTo("x");

        CocktailPrice price = cocktail.getPrices().iterator().next();
        assertThat(price.getPrice()).isNotNull();
        assertThat(price.getSize()).isNotNull();
        assertThat(price.isActive()).isTrue();
        assertThat(price.getCreatedAt()).isNotNull();
        assertThat(price.getUpdatedAt()).isNotNull();
        assertThat(price.getId()).isNotNull();
        assertThat(price).isEqualTo(price).isNotEqualTo("x");
    }

    @Test
    void cocktailIngredientAssociationIsMapped() {
        List<CocktailIngredient> associations = cocktailIngredientRepository.findAll();
        assertThat(associations).isNotEmpty();

        CocktailIngredient association = associations.get(0);
        assertThat(association.getId()).isNotNull();
        assertThat(association.getDisplayOrder()).isGreaterThanOrEqualTo(0);
        // quantity_label may legitimately be present in the seed.
        assertThat(association.getQuantityLabel()).isNotNull();
        assertThat(association).isEqualTo(association).isNotEqualTo("x");
        assertThat(association.hashCode()).isNotZero();
    }

    @Test
    void appUserIsMappedAheadOfAuthenticationPass() {
        // The seed leaves app_user empty; insert one row to validate the mapping.
        jdbcTemplate.update("""
                INSERT INTO app_user (username, password_hash, display_name, role, active)
                VALUES ('mapping_check', '$2a$10$placeholderhashplaceholderhashpl', 'Mapping Check', 'BARMAKER', true)
                """);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE username = 'mapping_check'", Long.class);

        AppUser user = appUserRepository.findById(id).orElseThrow();
        assertThat(user.getUsername()).isEqualTo("mapping_check");
        assertThat(user.getPasswordHash()).startsWith("$2a$");
        assertThat(user.getDisplayName()).isEqualTo("Mapping Check");
        assertThat(user.getRole()).isEqualTo(UserRole.BARMAKER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user).isEqualTo(appUserRepository.findById(id).orElseThrow())
                .hasSameHashCodeAs(appUserRepository.findById(id).orElseThrow())
                .isNotEqualTo("x");
    }
}
