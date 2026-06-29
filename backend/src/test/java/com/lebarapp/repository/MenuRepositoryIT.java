package com.lebarapp.repository;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.entity.Category;
import com.lebarapp.entity.Cocktail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository access against the seeded PostgreSQL catalog: confirms that only
 * active rows are returned and that categories come back in the expected order.
 */
class MenuRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CocktailRepository cocktailRepository;

    @Test
    void returnsOnlyActiveCategoriesOrdered() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();

        assertThat(categories).allMatch(Category::isActive);
        assertThat(categories).extracting(Category::getName)
                .containsExactly("Classiques", "Tropicaux", "Mocktails");
    }

    @Test
    void returnsOnlyActiveCocktailsFromActiveCategories() {
        List<Cocktail> cocktails = cocktailRepository.findActiveForMenu();

        assertThat(cocktails).isNotEmpty();
        assertThat(cocktails).allMatch(Cocktail::isActive);
        assertThat(cocktails).allMatch(c -> c.getCategory().isActive());
        assertThat(cocktails).extracting(Cocktail::getName)
                .doesNotContain("Cocktail retiré");
    }
}
