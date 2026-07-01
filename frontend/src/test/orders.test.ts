import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useCustomerOrderStore } from '@/stores/customerOrder';
import { ApiError } from '@/services/apiClient';
import { customerOrder } from './fixtures/catalog';

vi.mock('@/services/customerOrderApi', () => ({
  createOrder: vi.fn(),
  fetchOrder: vi.fn(),
}));
import * as api from '@/services/customerOrderApi';

describe('customer order store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(api.createOrder).mockReset();
    vi.mocked(api.fetchOrder).mockReset();
  });

  it('submits an order and keeps the returned id', async () => {
    const created = customerOrder();
    vi.mocked(api.createOrder).mockResolvedValue(created);
    const store = useCustomerOrderStore();
    const result = await store.submit({ items: [{ cocktailId: 101, size: 'M' }], tableNumber: 12, paymentMethod: 'CARD_AT_COUNTER' });
    expect(result.id).toBe(created.id);
    expect(store.lastOrderId).toBe(created.id);
  });

  it('surfaces a French error and rethrows on submit failure', async () => {
    vi.mocked(api.createOrder).mockRejectedValue(new ApiError({ message: 'x', status: 409, code: 'COCKTAIL_UNAVAILABLE' }));
    const store = useCustomerOrderStore();
    await expect(store.submit({ items: [], tableNumber: 12, paymentMethod: 'CARD_AT_COUNTER' })).rejects.toBeInstanceOf(ApiError);
    expect(store.submitError).toContain('plus disponible');
  });

  it('loads an order by id', async () => {
    vi.mocked(api.fetchOrder).mockResolvedValue(customerOrder());
    const store = useCustomerOrderStore();
    await store.loadOrder('id', { initial: true });
    expect(store.order?.publicCode).toBe('BA-1042');
    expect(store.notFound).toBe(false);
  });

  it('marks notFound on a missing order', async () => {
    vi.mocked(api.fetchOrder).mockRejectedValue(new ApiError({ message: 'x', status: 404, code: 'ORDER_NOT_FOUND' }));
    const store = useCustomerOrderStore();
    await store.loadOrder('missing', { initial: true });
    expect(store.notFound).toBe(true);
    expect(store.order).toBeNull();
  });

  it('keeps the visible order on a transient background failure', async () => {
    vi.mocked(api.fetchOrder).mockResolvedValueOnce(customerOrder());
    const store = useCustomerOrderStore();
    await store.loadOrder('id', { initial: true });
    vi.mocked(api.fetchOrder).mockRejectedValueOnce(new ApiError({ message: 'net', status: 0, isNetworkError: true }));
    await store.loadOrder('id', { initial: false });
    expect(store.order?.publicCode).toBe('BA-1042');
  });

  it('prevents an older response from overwriting a newer one', async () => {
    const store = useCustomerOrderStore();
    let resolveOld: (v: ReturnType<typeof customerOrder>) => void = () => {};
    const oldPromise = new Promise<ReturnType<typeof customerOrder>>((r) => { resolveOld = r; });
    vi.mocked(api.fetchOrder).mockReturnValueOnce(oldPromise);
    const first = store.loadOrder('id', { initial: true });

    vi.mocked(api.fetchOrder).mockResolvedValueOnce(customerOrder({ status: 'COMPLETED' }));
    await store.loadOrder('id', { initial: false });
    expect(store.order?.status).toBe('COMPLETED');

    resolveOld(customerOrder({ status: 'ORDERED' }));
    await first;
    expect(store.order?.status).toBe('COMPLETED'); // stale response ignored
  });
});
