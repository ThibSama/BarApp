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

    /**
     * Factory for a join row linking an (already persisted) cocktail and
     * ingredient. The composite id is initialized explicitly from both ids: the
     * embeddable instance must already exist for {@code @MapsId} to populate it
     * at persist time, and a non-null key keeps {@link #equals(Object)} stable.
     * Fresh rows are inserted via {@code EntityManager.persist} (not
     * {@code save}, which would {@code merge} an assigned-id entity).
     */
    public static CocktailIngredient create(Cocktail cocktail, Ingredient ingredient,
                                            int displayOrder, String quantityLabel) {
        CocktailIngredient association = new CocktailIngredient();
        association.id = new CocktailIngredientId(cocktail.getId(), ingredient.getId());
        association.cocktail = cocktail;
        association.ingredient = ingredient;
        association.displayOrder = displayOrder;
        association.quantityLabel = quantityLabel;
        return association;
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
