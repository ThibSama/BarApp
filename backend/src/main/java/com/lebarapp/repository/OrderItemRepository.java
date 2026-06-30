package com.lebarapp.repository;

import com.lebarapp.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Resolves the parent order id of an item without loading the item or its
     * lazy associations. Used by the preparation flow to acquire the aggregate
     * lock on the owning order before mutating the item; an empty result means
     * the item does not exist (404).
     */
    @Query("select i.order.id from OrderItem i where i.id = :itemId")
    Optional<UUID> findOrderIdById(@Param("itemId") UUID itemId);
}
