package com.lebarapp.mapper;

import com.lebarapp.enums.PaymentMethod;

import com.lebarapp.dto.OrderItemResponse;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.entity.OrderItem;
import com.lebarapp.enums.CocktailSize;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link OrderMapper}: items are always returned sorted by
 * sequence number and snapshot fields are carried through untouched.
 */
class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void mapsOrderAndSortsItemsBySequenceNumber() {
        CustomerOrder order = new CustomerOrder(UUID.randomUUID(), "ABC234", new BigDecimal("19.00"), 12, PaymentMethod.CARD_IN_APP);
        // Added out of sequence order on purpose.
        order.addItem(new OrderItem(UUID.randomUUID(), order, null, "Piña Colada",
                CocktailSize.S, new BigDecimal("9.50"), 2));
        order.addItem(new OrderItem(UUID.randomUUID(), order, null, "Mojito",
                CocktailSize.M, new BigDecimal("10.50"), 1));

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.publicCode()).isEqualTo("ABC234");
        assertThat(response.totalAmount()).isEqualByComparingTo("19.00");
        assertThat(response.items()).extracting(OrderItemResponse::sequenceNumber)
                .containsExactly(1, 2);
        assertThat(response.items()).extracting(OrderItemResponse::cocktailName)
                .containsExactly("Mojito", "Piña Colada");
        assertThat(response.items()).allSatisfy(i -> assertThat(i.completedAt()).isNull());
    }
}
