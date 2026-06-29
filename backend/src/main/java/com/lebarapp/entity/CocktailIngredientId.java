package com.lebarapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link CocktailIngredient} mirroring the
 * {@code (cocktail_id, ingredient_id)} primary key of the join table.
 */
@Embeddable
public class CocktailIngredientId implements Serializable {

    @Column(name = "cocktail_id")
    private Long cocktailId;

    @Column(name = "ingredient_id")
    private Long ingredientId;

    protected CocktailIngredientId() {
    }

    public CocktailIngredientId(Long cocktailId, Long ingredientId) {
        this.cocktailId = cocktailId;
        this.ingredientId = ingredientId;
    }

    public Long getCocktailId() {
        return cocktailId;
    }

    public Long getIngredientId() {
        return ingredientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CocktailIngredientId other)) {
            return false;
        }
        return Objects.equals(cocktailId, other.cocktailId)
                && Objects.equals(ingredientId, other.ingredientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cocktailId, ingredientId);
    }
}
