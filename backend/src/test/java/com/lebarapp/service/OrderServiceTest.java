package com.lebarapp.service;

import com.lebarapp.dto.CreateOrderItemRequest;
import com.lebarapp.dto.CreateOrderRequest;
import com.lebarapp.entity.Cocktail;
import com.lebarapp.entity.CocktailPrice;
import com.lebarapp.entity.CustomerOrder;
import com.lebarapp.entity.OrderItem;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.exception.CocktailNotFoundException;
import com.lebarapp.exception.CocktailUnavailableException;
import com.lebarapp.exception.PriceUnavailableException;
import com.lebarapp.exception.PublicCodeGenerationException;
import com.lebarapp.exception.SizeUnavailableException;
import com.lebarapp.mapper.OrderMapper;
import com.lebarapp.repository.CocktailRepository;
import com.lebarapp.repository.CustomerOrderRepository;
import com.lebarapp.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Deterministic unit tests for {@link OrderService}: catalog validation branches,
 * server-side total/sequence/snapshot computation, public-code collision retry
 * and the bounded-retry failure. Catalog entities are mocked, so no database is
 * involved and prices cannot be influenced by the request.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CocktailRepository cocktailRepository;
    @Mock
    private CustomerOrderRepository customerOrderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PublicCodeGenerator publicCodeGenerator;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<CustomerOrder> orderCaptor;

    private Cocktail mojito;

    @BeforeEach
    void setUp() {
        mojito = cocktailWithPrices(1L, "Mojito", true,
                activePrice(CocktailSize.S, "8.50", true),
                activePrice(CocktailSize.M, "10.50", true),
                activePrice(CocktailSize.L, "12.50", false)); // L inactive
    }

    @Test
    void buildsOrderWithServerSidePricesSequencesAndSnapshots() {
        when(cocktailRepository.findWithPricesById(1L)).thenReturn(Optional.of(mojito));
        when(customerOrderRepository.existsByPublicCode(any())).thenReturn(false);
        when(publicCodeGenerator.generate()).thenReturn("ABC234");

        // Two identical drinks + nothing else -> two separate items.
        orderService.createOrder(request(item(1L, CocktailSize.M), item(1L, CocktailSize.M)));

        verify(customerOrderRepository).save(orderCaptor.capture());
        CustomerOrder saved = orderCaptor.getValue();

        assertThat(saved.getPublicCode()).isEqualTo("ABC234");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("21.00");
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems()).extracting(OrderItem::getSequenceNumber).containsExactly(1, 2);
        assertThat(saved.getItems()).allSatisfy(i -> {
            assertThat(i.getCocktailNameSnapshot()).isEqualTo("Mojito");
            assertThat(i.getUnitPriceSnapshot()).isEqualByComparingTo("10.50");
            assertThat(i.getCompletedAt()).isNull();
        });
        verify(orderItemRepository).saveAll(any());
    }

    @Test
    void retriesOnPublicCodeCollisionThenSucceeds() {
        when(cocktailRepository.findWithPricesById(1L)).thenReturn(Optional.of(mojito));
        when(publicCodeGenerator.generate()).thenReturn("DUP123", "OK4567");
        when(customerOrderRepository.existsByPublicCode("DUP123")).thenReturn(true);
        when(customerOrderRepository.existsByPublicCode("OK4567")).thenReturn(false);

        orderService.createOrder(request(item(1L, CocktailSize.S)));

        verify(customerOrderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getPublicCode()).isEqualTo("OK4567");
    }

    @Test
    void throwsWhenPublicCodeCollisionRetriesExhausted() {
        when(cocktailRepository.findWithPricesById(1L)).thenReturn(Optional.of(mojito));
        when(publicCodeGenerator.generate()).thenReturn("DUP123");
        when(customerOrderRepository.existsByPublicCode("DUP123")).thenReturn(true);

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, CocktailSize.S))))
                .isInstanceOf(PublicCodeGenerationException.class);

        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void rejectsMissingCocktail() {
        when(cocktailRepository.findWithPricesById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request(item(99L, CocktailSize.M))))
                .isInstanceOf(CocktailNotFoundException.class);
        verify(customerOrderRepository, never()).save(any());
    }

    @Test
    void rejectsInactiveCocktail() {
        Cocktail inactive = cocktailWithPrices(2L, "Retiré", false,
                activePrice(CocktailSize.M, "10.00", true));
        when(cocktailRepository.findWithPricesById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> orderService.createOrder(request(item(2L, CocktailSize.M))))
                .isInstanceOf(CocktailUnavailableException.class);
    }

    @Test
    void rejectsUnavailableSize() {
        Cocktail onlyMedium = cocktailWithPrices(1L, "Mojito", true,
                activePrice(CocktailSize.M, "10.50", true));
        when(cocktailRepository.findWithPricesById(1L)).thenReturn(Optional.of(onlyMedium));

        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, CocktailSize.L))))
                .isInstanceOf(SizeUnavailableException.class);
    }

    @Test
    void rejectsInactivePrice() {
        when(cocktailRepository.findWithPricesById(1L)).thenReturn(Optional.of(mojito));

        // Size L exists on the mock but is inactive.
        assertThatThrownBy(() -> orderService.createOrder(request(item(1L, CocktailSize.L))))
                .isInstanceOf(PriceUnavailableException.class);
    }

    // --- helpers -----------------------------------------------------------

    private static CreateOrderRequest request(CreateOrderItemRequest... items) {
        return new CreateOrderRequest(List.of(items));
    }

    private static CreateOrderItemRequest item(Long cocktailId, CocktailSize size) {
        return new CreateOrderItemRequest(cocktailId, size);
    }

    private static Cocktail cocktailWithPrices(Long id, String name, boolean active, CocktailPrice... prices) {
        Cocktail cocktail = org.mockito.Mockito.mock(Cocktail.class);
        // Lenient throughout: the shared "mojito" fixture is built in setUp() but
        // not exercised by every test, so strict stubbing would flag it.
        org.mockito.Mockito.lenient().when(cocktail.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(cocktail.isActive()).thenReturn(active);
        org.mockito.Mockito.lenient().when(cocktail.getName()).thenReturn(name);
        org.mockito.Mockito.lenient().when(cocktail.getPrices()).thenReturn(Set.of(prices));
        return cocktail;
    }

    private static CocktailPrice activePrice(CocktailSize size, String price, boolean active) {
        CocktailPrice cocktailPrice = org.mockito.Mockito.mock(CocktailPrice.class);
        org.mockito.Mockito.lenient().when(cocktailPrice.getSize()).thenReturn(size);
        org.mockito.Mockito.lenient().when(cocktailPrice.getPrice()).thenReturn(new BigDecimal(price));
        org.mockito.Mockito.lenient().when(cocktailPrice.isActive()).thenReturn(active);
        return cocktailPrice;
    }
}
