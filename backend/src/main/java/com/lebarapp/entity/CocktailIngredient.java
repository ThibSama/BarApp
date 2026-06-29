package com.lebarapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Association entity between {@link Cocktail} and {@link Ingredient}. Modelled
 * explicitly (rather than a plain {@code @ManyToMany}) because the join row
 * carries its own attributes: {@code display_order} and {@code quantity_label}.
 */
@Entity
@Table(name = "cocktail_ingredient")
public class CocktailIngredient {

    @EmbeddedId
    private CocktailIngredientId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("cocktailId")
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "quantity_label", length = 80)
    private String quantityLabel;

    protected CocktailIngredient() {
    }

    public CocktailIngredientId getId() {
        return id;
    }

    public Cocktail getCocktail() {
        return cocktail;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getQuantityLabel() {
        return quantityLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CocktailIngredient other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
