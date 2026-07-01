package com.lebarapp.entity;

import com.lebarapp.enums.CocktailSize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "cocktail_price")
public class CocktailPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    // Force VARCHAR(1): a length-1 enum column would otherwise be validated as
    // CHAR(1) by Hibernate, clashing with the VARCHAR(1) defined in the DDL.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 1)
    private CocktailSize size;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    protected CocktailPrice() {
    }

    /** Factory for a new active price line for one size of a cocktail. */
    public static CocktailPrice create(Cocktail cocktail, CocktailSize size, BigDecimal price) {
        CocktailPrice cocktailPrice = new CocktailPrice();
        cocktailPrice.cocktail = cocktail;
        cocktailPrice.size = size;
        cocktailPrice.price = price;
        cocktailPrice.active = true;
        return cocktailPrice;
    }

    /**
     * Updates the amount of an existing size in place and (re)activates it, so a
     * price replacement keeps exactly one active row per size under the
     * {@code (cocktail_id, size)} unique constraint.
     */
    public void update(BigDecimal price) {
        this.price = price;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public Cocktail getCocktail() {
        return cocktail;
    }

    public CocktailSize getSize() {
        return size;
    }

    public BigDecimal getPrice() {
        return price;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CocktailPrice other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
