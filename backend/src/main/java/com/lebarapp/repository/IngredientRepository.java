package com.lebarapp.repository;

import com.lebarapp.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    /**
     * Case-insensitive lookup used to reuse an existing ingredient instead of
     * creating a duplicate (backed by the {@code LOWER(name)} unique index).
     * This is the single, shared ingredient-resolution lookup used both by the
     * cocktail aggregate and by the autonomous ingredient administration.
     */
    Optional<Ingredient> findByNameIgnoreCase(String name);

    /**
     * Management listing: every ingredient (active and inactive), active first,
     * then by name (case-insensitive), then id as a deterministic tie-breaker.
     */
    List<Ingredient> findAllByOrderByActiveDescNameAscIdAsc();

    /** Case-insensitive uniqueness check used on ingredient creation. */
    boolean existsByNameIgnoreCase(String name);

    /** Case-insensitive uniqueness check on update, excluding the edited row. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
