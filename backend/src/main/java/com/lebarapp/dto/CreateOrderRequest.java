package com.lebarapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Anonymous client order request. Each element of {@code items} represents one
 * physical drink; the same cocktail/size may appear several times and yields one
 * {@link com.lebarapp.entity.OrderItem} per occurrence.
 *
 * <p>The client never provides prices, names, totals, status or sequence
 * numbers — these are computed entirely server-side.</p>
 */
public record CreateOrderRequest(
        @NotNull
        @Size(min = 1, max = 50, message = "Une commande doit contenir entre 1 et 50 boissons.")
        List<@NotNull(message = "Un élément de commande ne peut pas être nul.") @Valid CreateOrderItemRequest> items) {
}
