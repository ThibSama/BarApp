package com.lebarapp.repository;

import com.lebarapp.entity.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    /** Cheap existence check used by the public-code generator's collision retry. */
    boolean existsByPublicCode(String publicCode);

    /**
     * Loads an order together with its items in a single query (entity graph),
     * so tracking reads never trigger lazy-loading failures or per-item N+1
     * queries. The DTO mapping only reads each item's snapshot fields, so the
     * (lazy) cocktail association is intentionally not fetched. Item ordering by
     * {@code sequenceNumber} is applied in the mapping layer.
     */
    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findWithItemsById(UUID id);
}
