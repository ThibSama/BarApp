package com.lebarapp.repository;

import com.lebarapp.entity.CocktailPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CocktailPriceRepository extends JpaRepository<CocktailPrice, Long> {

    /**
     * Loads the existing price lines of a cocktail so the "replace prices"
     * operation can upsert one active row per size in place, respecting the
     * {@code (cocktail_id, size)} unique constraint.
     */
    List<CocktailPrice> findByCocktailId(Long cocktailId);
}
