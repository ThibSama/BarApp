package com.lebarapp.service;

import com.lebarapp.dto.CreateOrderItemRequest;
import com.lebarapp.dto.CreateOrderRequest;
import com.lebarapp.dto.OrderResponse;
import com.lebarapp.entity.Cocktail;
import com.lebarapp.entity.CocktailPrice;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.entity.OrderItem;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.exception.CocktailNotFoundException;
import com.lebarapp.exception.CocktailUnavailableException;
import com.lebarapp.exception.OrderNotFoundException;
import com.lebarapp.exception.PriceUnavailableException;
import com.lebarapp.exception.PublicCodeGenerationException;
import com.lebarapp.exception.SizeUnavailableException;
import com.lebarapp.mapper.OrderMapper;
import com.lebarapp.repository.CocktailRepository;
import com.lebarapp.repository.CustomerOrderRepository;
import com.lebarapp.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read/write service for the anonymous client order flow. Order creation runs in
 * a single transaction: the catalog is validated, prices are read exclusively
 * server-side, snapshots are frozen, and the order plus its items are persisted
 * atomically. Any invalid item rolls the whole operation back, so no partial
 * order is ever left in the database.
 */
@Service
public class OrderService {

    /** Bounded retry budget for public-code collisions (no infinite loop). */
    static final int MAX_PUBLIC_CODE_ATTEMPTS = 5;

    private final CocktailRepository cocktailRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PublicCodeGenerator publicCodeGenerator;
    private final OrderMapper orderMapper;

    public OrderService(CocktailRepository cocktailRepository,
                        CustomerOrderRepository customerOrderRepository,
                        OrderItemRepository orderItemRepository,
                        PublicCodeGenerator publicCodeGenerator,
                        OrderMapper orderMapper) {
        this.cocktailRepository = cocktailRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.publicCodeGenerator = publicCodeGenerator;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        List<CreateOrderItemRequest> requestedItems = request.items();

        // Load each distinct cocktail once (with its prices) and validate it is
        // active, so a cart with duplicate drinks does not re-query the catalog.
        Map<Long, Cocktail> cocktailsById = loadAndValidateCocktails(requestedItems);

        // Resolve the server-side unit price for every requested line, compute the
        // exact total with BigDecimal, then build the snapshot lines.
        BigDecimal total = BigDecimal.ZERO;
        List<ResolvedLine> resolvedLines = new ArrayList<>(requestedItems.size());
        for (CreateOrderItemRequest requested : requestedItems) {
            Cocktail cocktail = cocktailsById.get(requested.cocktailId());
            BigDecimal unitPrice = resolveActivePrice(cocktail, requested.size());
            total = total.add(unitPrice);
            resolvedLines.add(new ResolvedLine(cocktail, requested.size(), unitPrice));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        CustomerOrder order = new CustomerOrder(UUID.randomUUID(), generateUniquePublicCode(), total);

        List<OrderItem> items = new ArrayList<>(resolvedLines.size());
        int sequenceNumber = 1;
        for (ResolvedLine line : resolvedLines) {
            OrderItem item = new OrderItem(
                    UUID.randomUUID(),
                    order,
                    line.cocktail(),
                    line.cocktail().getName(),   // historical name snapshot
                    line.size(),
                    line.unitPrice(),            // historical price snapshot
                    sequenceNumber++);
            order.addItem(item);
            items.add(item);
        }

        // No JPA cascade: persist the order then its items explicitly, then flush
        // so the DB-generated createdAt is read back for the response.
        customerOrderRepository.save(order);
        orderItemRepository.saveAll(items);
        customerOrderRepository.flush();

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        CustomerOrder order = customerOrderRepository.findWithItemsById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        return orderMapper.toResponse(order);
    }

    private Map<Long, Cocktail> loadAndValidateCocktails(List<CreateOrderItemRequest> requestedItems) {
        Map<Long, Cocktail> cocktailsById = new HashMap<>();
        for (CreateOrderItemRequest requested : requestedItems) {
            Long cocktailId = requested.cocktailId();
            if (cocktailsById.containsKey(cocktailId)) {
                continue;
            }
            Cocktail cocktail = cocktailRepository.findWithPricesById(cocktailId)
                    .orElseThrow(() -> new CocktailNotFoundException(cocktailId));
            if (!cocktail.isActive()) {
                throw new CocktailUnavailableException(cocktailId);
            }
            cocktailsById.put(cocktailId, cocktail);
        }
        return cocktailsById;
    }

    private BigDecimal resolveActivePrice(Cocktail cocktail, CocktailSize size) {
        CocktailPrice match = cocktail.getPrices().stream()
                .filter(price -> price.getSize() == size)
                .findFirst()
                .orElseThrow(() -> new SizeUnavailableException(cocktail.getId(), size));
        if (!match.isActive()) {
            throw new PriceUnavailableException(cocktail.getId(), size);
        }
        return match.getPrice();
    }

    private String generateUniquePublicCode() {
        for (int attempt = 1; attempt <= MAX_PUBLIC_CODE_ATTEMPTS; attempt++) {
            String candidate = publicCodeGenerator.generate();
            if (!customerOrderRepository.existsByPublicCode(candidate)) {
                return candidate;
            }
        }
        throw new PublicCodeGenerationException();
    }

    /** Internal carrier for a validated request line and its server-side price. */
    private record ResolvedLine(Cocktail cocktail, CocktailSize size, BigDecimal unitPrice) {
    }
}
