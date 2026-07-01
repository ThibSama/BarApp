package com.lebarapp.repository;

import com.lebarapp.entity.CocktailIngredient;
import com.lebarapp.entity.CocktailIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailIngredientRepository
        extends JpaRepository<CocktailIngredient, CocktailIngredientId> {

    /**
     * Loads the current ingredient associations of a cocktail. The transactional
     * "replace ingredients" operation deletes these managed rows and flushes
     * before inserting the deterministic fresh set, keeping the persistence
     * context in sync so a reused ingredient does not collide on the
     * {@code (cocktail_id, ingredient_id)} primary key.
     */
    List<CocktailIngredient> findByIdCocktailId(Long cocktailId);
}
