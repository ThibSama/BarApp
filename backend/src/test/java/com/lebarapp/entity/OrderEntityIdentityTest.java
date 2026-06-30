package com.lebarapp.entity;

import com.lebarapp.enums.PaymentMethod;

import com.lebarapp.enums.CocktailSize;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Identity (id-based {@code equals}/{@code hashCode}) and accessor checks for the
 * order aggregate and the composite key. Identity semantics are relied upon when
 * entities live in hash-based collections and Hibernate persistence contexts.
 */
class OrderEntityIdentityTest {

    @Test
    void customerOrderEqualityIsIdBased() {
        UUID id = UUID.randomUUID();
        CustomerOrder a = new CustomerOrder(id, "AAA111", new BigDecimal("10.00"), 12, PaymentMethod.CARD_IN_APP);
        CustomerOrder sameId = new CustomerOrder(id, "BBB222", new BigDecimal("99.00"), 12, PaymentMethod.CARD_IN_APP);
        CustomerOrder other = new CustomerOrder(UUID.randomUUID(), "CCC333", new BigDecimal("10.00"), 12, PaymentMethod.CARD_IN_APP);

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(sameId);
        assertThat(a).hasSameHashCodeAs(sameId);
        assertThat(a).isNotEqualTo(other);
        assertThat(a).isNotEqualTo("not an order");
        assertThat(new CustomerOrder(null, "X", BigDecimal.ZERO, 12, PaymentMethod.CARD_IN_APP)).isNotEqualTo(a);

        // Accessors not exercised by the mapper happy path.
        assertThat(a.getUpdatedAt()).isNull();
        assertThat(a.getCompletedAt()).isNull();
        assertThat(a.isNew()).isTrue();
        assertThat(a.getItems()).isEmpty();
    }

    @Test
    void orderItemEqualityIsIdBasedAndExposesAssociations() {
        UUID id = UUID.randomUUID();
        CustomerOrder order = new CustomerOrder(UUID.randomUUID(), "AAA111", new BigDecimal("10.50"), 12, PaymentMethod.CARD_IN_APP);
        OrderItem a = new OrderItem(id, order, null, "Mojito", CocktailSize.M, new BigDecimal("10.50"), 1);
        OrderItem sameId = new OrderItem(id, order, null, "Other", CocktailSize.S, new BigDecimal("8.50"), 2);
        OrderItem other = new OrderItem(UUID.randomUUID(), order, null, "Mojito", CocktailSize.M,
                new BigDecimal("10.50"), 1);

        assertThat(a).isEqualTo(a).isEqualTo(sameId).isNotEqualTo(other).isNotEqualTo("x");
        assertThat(a).hasSameHashCodeAs(sameId);

        assertThat(a.getOrder()).isSameAs(order);
        assertThat(a.getCocktail()).isNull();
        assertThat(a.getCreatedAt()).isNull();
        assertThat(a.getUpdatedAt()).isNull();
        assertThat(a.getUnitPriceSnapshot()).isEqualByComparingTo("10.50");
        assertThat(a.isNew()).isTrue();
    }

    @Test
    void cocktailIngredientIdEqualityAndAccessors() {
        CocktailIngredientId a = new CocktailIngredientId(1L, 2L);
        CocktailIngredientId same = new CocktailIngredientId(1L, 2L);
        CocktailIngredientId diff = new CocktailIngredientId(1L, 3L);

        assertThat(a).isEqualTo(a);
        assertThat(a).isEqualTo(same);
        assertThat(a).hasSameHashCodeAs(same);
        assertThat(a).isNotEqualTo(diff);
        assertThat(a).isNotEqualTo("x");
        assertThat(a.getCocktailId()).isEqualTo(1L);
        assertThat(a.getIngredientId()).isEqualTo(2L);
    }
}
