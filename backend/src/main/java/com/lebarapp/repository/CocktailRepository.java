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
}
