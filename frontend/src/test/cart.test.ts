import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useCartStore, type AddCartItemInput } from '@/stores/cart';
import { CART_SCHEMA_VERSION } from '@/types/cart';

const CART_KEY = 'barapp.cart.v2';

const mojitoM: AddCartItemInput = { cocktailId: 101, name: 'Mojito', size: 'M', unitPrice: 8.5, imageUrl: null };
const mojitoL: AddCartItemInput = { cocktailId: 101, name: 'Mojito', size: 'L', unitPrice: 10.5, imageUrl: null };

describe('cart store', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
  });

  it('adds a real cocktail with snapshots and computes the total', () => {
    const cart = useCartStore();
    cart.addItem(mojitoM, 2);
    expect(cart.itemCount).toBe(2);
    expect(cart.total).toBe(17);
    expect(cart.items[0]).toMatchObject({ cocktailId: 101, nameSnapshot: 'Mojito', size: 'M', unitPriceSnapshot: 8.5 });
  });

  it('increments the same cocktail/size and separates different sizes', () => {
    const cart = useCartStore();
    cart.addItem(mojitoM, 1);
    cart.addItem(mojitoM, 1);
    expect(cart.items).toHaveLength(1);
    expect(cart.items[0].quantity).toBe(2);
    cart.addItem(mojitoL, 1);
    expect(cart.items).toHaveLength(2);
  });

  it('updates quantity and removes lines', () => {
    const cart = useCartStore();
    cart.addItem(mojitoM, 1);
    cart.updateQuantity(cart.items[0].id, 3);
    expect(cart.items[0].quantity).toBe(3);
    cart.updateQuantity(cart.items[0].id, 0);
    expect(cart.items).toHaveLength(0);
  });

  it('expands quantities into one order item per physical drink', () => {
    const cart = useCartStore();
    cart.addItem(mojitoM, 3);
    cart.addItem(mojitoL, 1);
    const items = cart.toOrderItems();
    expect(items).toHaveLength(4);
    expect(items.filter((i) => i.size === 'M')).toHaveLength(3);
    expect(items).toContainEqual({ cocktailId: 101, size: 'L' });
  });

  it('restores a valid persisted cart', () => {
    localStorage.setItem(
      CART_KEY,
      JSON.stringify({
        version: CART_SCHEMA_VERSION,
        lines: [{ id: 'l1', cocktailId: 101, nameSnapshot: 'Mojito', size: 'M', unitPriceSnapshot: 8.5, quantity: 2, imageUrlSnapshot: null }],
      }),
    );
    const cart = useCartStore();
    expect(cart.items).toHaveLength(1);
    expect(cart.itemCount).toBe(2);
  });

  it('discards incompatible old persistence (slug ids / wrong version / bad qty)', () => {
    // Old mock cart shape: string cocktailId, no price snapshot, no version.
    localStorage.setItem(
      CART_KEY,
      JSON.stringify({ version: 1, lines: [{ id: 'x', cocktailId: 'mojito', size: 'M', quantity: 1 }] }),
    );
    expect(useCartStore().items).toHaveLength(0);

    setActivePinia(createPinia());
    localStorage.setItem(
      CART_KEY,
      JSON.stringify({
        version: CART_SCHEMA_VERSION,
        lines: [
          { id: 'a', cocktailId: 101, nameSnapshot: 'Mojito', size: 'XL', unitPriceSnapshot: 8.5, quantity: 1, imageUrlSnapshot: null },
          { id: 'b', cocktailId: 101, nameSnapshot: 'Mojito', size: 'M', unitPriceSnapshot: 8.5, quantity: 0, imageUrlSnapshot: null },
          { id: 'c', cocktailId: 101, nameSnapshot: 'Mojito', size: 'M', unitPriceSnapshot: 8.5, quantity: 1, imageUrlSnapshot: null },
        ],
      }),
    );
    const cart = useCartStore();
    expect(cart.items).toHaveLength(1);
    expect(cart.items[0].id).toBe('c');
  });

  it('never crashes on malformed JSON', () => {
    localStorage.setItem(CART_KEY, '{not json');
    expect(useCartStore().items).toEqual([]);
  });
});
