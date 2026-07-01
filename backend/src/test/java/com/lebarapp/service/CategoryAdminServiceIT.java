package com.lebarapp.service;

import com.lebarapp.AbstractPostgresIntegrationTest;
import com.lebarapp.dto.CategoryRequest;
import com.lebarapp.dto.CategoryResponse;
import com.lebarapp.dto.MenuResponse;
import com.lebarapp.exception.CategoryAlreadyExistsException;
import com.lebarapp.exception.CategoryNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end category-management behaviour against the seeded PostgreSQL
 * database. Each test runs in a rolled-back transaction so created/modified
 * rows never leak into the seed-based fixtures used by the other IT classes
 * (e.g. {@link MenuServiceIT}).
 */
@Transactional
class CategoryAdminServiceIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private CategoryAdminService categoryService;
    @Autowired
    private MenuService menuService;

    @Test
    void listIncludesActiveAndInactiveCategories() {
        List<String> names = categoryService.list().stream().map(CategoryResponse::name).toList();
        // "Promotions expirées" is the seeded inactive category.
        assertThat(names).contains("Classiques", "Promotions expirées");
    }

    @Test
    void listIsDeterministicallyOrdered() {
        List<CategoryResponse> categories = categoryService.list();
        assertThat(categories).isSortedAccordingTo(
                Comparator.comparingInt(CategoryResponse::displayOrder)
                        .thenComparing(CategoryResponse::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CategoryResponse::id));
    }

    @Test
    void createPersistsCategory() {
        CategoryResponse created = categoryService.create(
                new CategoryRequest("Signatures", "Nos créations maison", 7, true));

        assertThat(created.id()).isNotNull();
        assertThat(created.active()).isTrue();
        assertThat(categoryService.list()).extracting(CategoryResponse::name).contains("Signatures");
    }

    @Test
    void createTrimsValuesAndDefaultsActive() {
        CategoryResponse created = categoryService.create(
                new CategoryRequest("  Apéritifs  ", "  léger  ", 5, null));

        assertThat(created.name()).isEqualTo("Apéritifs");
        assertThat(created.description()).isEqualTo("léger");
        assertThat(created.active()).isTrue();
    }

    @Test
    void blankDescriptionIsStoredAsNull() {
        CategoryResponse created = categoryService.create(
                new CategoryRequest("Eaux", "   ", 6, true));
        assertThat(created.description()).isNull();
    }

    @Test
    void duplicateNameIsRejectedCaseInsensitively() {
        assertThatThrownBy(() -> categoryService.create(
                new CategoryRequest("classiques", null, 8, true)))
                .isInstanceOf(CategoryAlreadyExistsException.class);
    }

    @Test
    void updateModifiesCategory() {
        CategoryResponse created = categoryService.create(
                new CategoryRequest("Brunch", "matin", 9, true));
        CategoryResponse updated = categoryService.update(created.id(),
                new CategoryRequest("Brunch & Co", "matinée", 3, true));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Brunch & Co");
        assertThat(updated.description()).isEqualTo("matinée");
        assertThat(updated.displayOrder()).isEqualTo(3);
    }

    @Test
    void updateUnknownIdThrows() {
        assertThatThrownBy(() -> categoryService.update(999_999L,
                new CategoryRequest("X", null, 1, true)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void updateToExistingNameIsRejected() {
        CategoryResponse a = categoryService.create(new CategoryRequest("Alpha", null, 10, true));
        categoryService.create(new CategoryRequest("Beta", null, 11, true));
        assertThatThrownBy(() -> categoryService.update(a.id(),
                new CategoryRequest("beta", null, 10, true)))
                .isInstanceOf(CategoryAlreadyExistsException.class);
    }

    @Test
    void deactivateSetsCategoryInactive() {
        CategoryResponse created = categoryService.create(new CategoryRequest("Temp", null, 12, true));
        categoryService.deactivate(created.id());

        CategoryResponse reloaded = categoryService.list().stream()
                .filter(c -> c.id().equals(created.id())).findFirst().orElseThrow();
        assertThat(reloaded.active()).isFalse();
    }

    @Test
    void deactivateUnknownIdThrows() {
        assertThatThrownBy(() -> categoryService.deactivate(999_999L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deactivatedCategoryDisappearsFromPublicMenu() {
        CategoryResponse created = categoryService.create(
                new CategoryRequest("Éphémère", null, 50, true));
        assertThat(menuCategoryNames()).contains("Éphémère");

        categoryService.deactivate(created.id());
        assertThat(menuCategoryNames()).doesNotContain("Éphémère");
    }

    @Test
    void reactivationThroughUpdateRestoresMenuVisibility() {
        CategoryResponse created = categoryService.create(
                new CategoryRequest("Saison", null, 51, true));
        categoryService.deactivate(created.id());
        assertThat(menuCategoryNames()).doesNotContain("Saison");

        categoryService.update(created.id(), new CategoryRequest("Saison", null, 51, true));
        assertThat(menuCategoryNames()).contains("Saison");
    }

    private List<String> menuCategoryNames() {
        MenuResponse menu = menuService.getMenu();
        return menu.categories().stream().map(c -> c.name()).toList();
    }
}
