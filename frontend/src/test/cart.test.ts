import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useCartStore } from '@/stores/cart';
import { useCatalogStore } from '@/stores/catalog';
import { useOrderStore } from '@/stores/orders';

describe('cart store', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('adds an item and calculates the total', () => {
    const cart = useCartStore();
    const catalog = useCatalogStore();
    const mojito = catalog.getCocktailById('mojito');
    cart.addItem('mojito', 'M', 2);
    expect(cart.itemCount).toBe(2);
    expect(cart.total).toBe((mojito?.prices.M ?? 0) * 2);
  });

  it('changes item quantity', () => {
    const cart = useCartStore();
    cart.addItem('mojito', 'S', 1);
    cart.updateQuantity(cart.items[0].id, 3);
    expect(cart.items[0].quantity).toBe(3);
  });

  it('removes an item', () => {
    const cart = useCartStore();
    cart.addItem('mojito', 'S', 1);
    cart.removeItem(cart.items[0].id);
    expect(cart.items).toHaveLength(0);
  });

  it('stores the selected payment method in the created order', () => {
    const cart = useCartStore();
    const orders = useOrderStore();
    cart.addItem('mojito', 'M', 1);
    const order = orders.createOrderFromCart('apple_pay');
    expect(order?.paymentMethod).toBe('apple_pay');
    expect(orders.orders[0].paymentMethod).toBe('apple_pay');
    expect(cart.items).toHaveLength(0);
  });
});
