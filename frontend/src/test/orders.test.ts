import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { useOrderStore } from '@/stores/orders';

describe('order store', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('marks an order as completed when all cocktails are completed', () => {
    const orders = useOrderStore();
    const order = orders.getOrderById('order-1')!;
    order.items.forEach((item) => { item.preparationStep = 'completed'; });
    orders.updateOrderStatus(order.id);
    expect(order.status).toBe('completed');
  });
});
