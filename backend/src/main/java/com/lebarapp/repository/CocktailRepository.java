package com.lebarapp.repository;

import com.lebarapp.entity.Cocktail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CocktailRepository extends JpaRepository<Cocktail, Long> {

    /**
     * Loads a single cocktail together with all of its prices in one query, used
     * by order creation to validate the requested size and read the current
     * server-side price. Each distinct cocktail is loaded once per request, so a
     * cart containing duplicate drinks does not re-query the catalog.
     */
    @EntityGraph(attributePaths = "prices")
    Optional<Cocktail> findWithPricesById(Long id);

    /**
     * Loads every active cocktail belonging to an active category, eagerly
     * fetching the category, the ingredient associations (with their ingredient)
     * and the prices in a single round trip. Using an entity graph over
     * {@code Set} collections avoids the N+1 problem without raising a
     * {@code MultipleBagFetchException}.
     *
     * <p>Filtering of inactive ingredients and inactive prices, as well as final
     * ordering, is performed in the mapping layer so that a cocktail without
     * active ingredients is still returned.</p>
     */
    @Query("select c from Cocktail c where c.active = true and c.category.active = true")
    @EntityGraph(attributePaths = {
            "category",
            "ingredients",
            "ingredients.ingredient",
            "prices"
    })
    List<Cocktail> findActiveForMenu();

    /**
     * Management listing: every cocktail (active and inactive, regardless of its
     * category's state), with category, ingredient associations and prices
     * eagerly fetched in a single round trip. {@code Set} collections keep this
     * free of {@code MultipleBagFetchException}; ordering is applied in the
     * service/mapping layer.
     */
    @Query("select c from Cocktail c")
    @EntityGraph(attributePaths = {
            "category",
            "ingredients",
            "ingredients.ingredient",
            "prices"
    })
    List<Cocktail> findAllForManagement();

    /**
     * Loads a single cocktail with category, ingredient associations and prices
     * for the management detail view (active and inactive alike).
     */
    @EntityGraph(attributePaths = {
            "category",
            "ingredients",
            "ingredients.ingredient",
            "prices"
    })
    Optional<Cocktail> findWithDetailById(Long id);

    /** Case-insensitive uniqueness of a cocktail name within its category. */
    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    /** Same as above, excluding the edited cocktail (used on update). */
    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);
}
