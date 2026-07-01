package com.lebarapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "cocktail")
public class Cocktail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    // Optional teaser shown on menu cards / edited in the barmaker UI (V4 column).
    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    // Read-only association mapping. Sets allow fetching both collections in a
    // single entity graph without a MultipleBagFetchException. No cascade: the
    // database owns the delete semantics (ON DELETE CASCADE in DDL).
    @OneToMany(mappedBy = "cocktail", fetch = FetchType.LAZY)
    private Set<CocktailIngredient> ingredients = new HashSet<>();

    @OneToMany(mappedBy = "cocktail", fetch = FetchType.LAZY)
    private Set<CocktailPrice> prices = new HashSet<>();

    protected Cocktail() {
    }

    /**
     * Factory for a new cocktail aggregate root. Children (ingredients/prices)
     * are persisted separately by the service through their own repositories;
     * this entity only owns its own scalar state and its category link.
     */
    public static Cocktail create(Category category, String name, String description,
                                  String shortDescription, String imageUrl, boolean active) {
        Cocktail cocktail = new Cocktail();
        cocktail.category = category;
        cocktail.name = name;
        cocktail.description = description;
        cocktail.shortDescription = shortDescription;
        cocktail.imageUrl = imageUrl;
        cocktail.active = active;
        return cocktail;
    }

    /** Updates the scalar details edited through the management API. */
    public void updateDetails(String name, String description, String shortDescription,
                              String imageUrl, boolean active) {
        this.name = name;
        this.description = description;
        this.shortDescription = shortDescription;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    /** Re-parents the cocktail to another (existing, active) category. */
    public void changeCategory(Category category) {
        this.category = category;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<CocktailIngredient> getIngredients() {
        return ingredients;
    }

    public Set<CocktailPrice> getPrices() {
        return prices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cocktail other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
