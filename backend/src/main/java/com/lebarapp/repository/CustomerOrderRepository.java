package com.lebarapp.repository;

import com.lebarapp.dto.BarOrderSummaryResponse;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    /**
     * Builds the barmaker queue summaries for the given statuses in a single
     * aggregate query: progress counters are computed in PostgreSQL via
     * {@code GROUP BY} so neither the items nor the cocktail entities are loaded.
     * Active orders are returned oldest-first ({@code createdAt} ascending, then
     * {@code id} as a stable tie-breaker).
     */
    @Query("""
            select new com.lebarapp.dto.BarOrderSummaryResponse(
                o.id, o.publicCode, o.status, o.totalAmount, o.tableNumber, o.createdAt, o.completedAt,
                count(i.id),
                coalesce(sum(case when i.preparationStatus = com.lebarapp.enums.PreparationStatus.COMPLETED then 1L else 0L end), 0L))
            from CustomerOrder o
            left join o.items i
            where o.status in :statuses
            group by o.id, o.publicCode, o.status, o.totalAmount, o.tableNumber, o.createdAt, o.completedAt
            order by o.createdAt asc, o.id asc
            """)
    List<BarOrderSummaryResponse> findActiveSummaries(@Param("statuses") Collection<OrderStatus> statuses);

    /**
     * Same aggregate summary as {@link #findActiveSummaries}, but ordered for the
     * completed history: most recently completed first ({@code completedAt}
     * descending, then {@code id} as a stable tie-breaker).
     */
    @Query("""
            select new com.lebarapp.dto.BarOrderSummaryResponse(
                o.id, o.publicCode, o.status, o.totalAmount, o.tableNumber, o.createdAt, o.completedAt,
                count(i.id),
                coalesce(sum(case when i.preparationStatus = com.lebarapp.enums.PreparationStatus.COMPLETED then 1L else 0L end), 0L))
            from CustomerOrder o
            left join o.items i
            where o.status in :statuses
            group by o.id, o.publicCode, o.status, o.totalAmount, o.tableNumber, o.createdAt, o.completedAt
            order by o.completedAt desc, o.id asc
            """)
    List<BarOrderSummaryResponse> findCompletedSummaries(@Param("statuses") Collection<OrderStatus> statuses);

    /**
     * Loads a single order row under a pessimistic write lock
     * ({@code SELECT ... FOR UPDATE}) so concurrent preparation updates on the
     * same order serialize on this row. The lock scope is intentionally the
     * single aggregate root: other orders in the queue are never locked. Items
     * are loaded lazily afterwards within the same transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") UUID id);
}
