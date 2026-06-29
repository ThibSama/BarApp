package com.lebarapp.repository;

import com.lebarapp.entity.CocktailIngredient;
import com.lebarapp.entity.CocktailIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CocktailIngredientRepository
        extends JpaRepository<CocktailIngredient, CocktailIngredientId> {
}
