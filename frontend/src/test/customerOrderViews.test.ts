import { render, screen } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import OrderConfirmationView from '@/views/client/OrderConfirmationView.vue';
import OrderTrackingView from '@/views/client/OrderTrackingView.vue';
import { ApiError } from '@/services/apiClient';
import { customerOrder } from './fixtures/catalog';

vi.mock('@/services/customerOrderApi', () => ({ createOrder: vi.fn(), fetchOrder: vi.fn() }));
import * as api from '@/services/customerOrderApi';

async function mount(component: unknown, path: string) {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/client/menu', name: 'client-menu', component: { template: '<div />' } },
      { path: '/client/confirmation/:orderId', name: 'client-order-confirmation', component: OrderConfirmationView },
      { path: '/client/suivi/:orderId', name: 'client-order-tracking', component: OrderTrackingView },
    ],
  });
  await router.push(path);
  await router.isReady();
  return render(component as never, { global: { plugins: [router] } });
}

describe('order confirmation view', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(api.fetchOrder).mockReset();
  });

  it('fetches the persisted order on direct reload and shows real data', async () => {
    vi.mocked(api.fetchOrder).mockResolvedValue(customerOrder());
    await mount(OrderConfirmationView, '/client/confirmation/abc');
    expect(await screen.findByText(/BA-1042/)).toBeTruthy();
    expect(screen.getByText('Commandée')).toBeTruthy();
    expect(screen.getByText(/Table :/).closest('span')?.textContent).toContain('12');
    expect(screen.getByText(/Mode de paiement/).closest('p')?.textContent).toContain('Carte bancaire au comptoir');
    expect(api.fetchOrder).toHaveBeenCalledWith('abc');
  });

  it('shows a not-found state on a missing order', async () => {
    vi.mocked(api.fetchOrder).mockRejectedValue(new ApiError({ message: 'x', status: 404, code: 'ORDER_NOT_FOUND' }));
    await mount(OrderConfirmationView, '/client/confirmation/missing');
    expect(await screen.findByText('Commande introuvable')).toBeTruthy();
  });
});

describe('order tracking view (polling)', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(api.fetchOrder).mockReset();
  });
  afterEach(() => vi.useRealTimers());

  it('polls and stops once the order is COMPLETED', async () => {
    vi.useFakeTimers();
    vi.mocked(api.fetchOrder).mockResolvedValue(customerOrder({ status: 'ORDERED' }));
    await mount(OrderTrackingView, '/client/suivi/abc');
    await vi.waitFor(() => expect(api.fetchOrder).toHaveBeenCalledTimes(1));

    await vi.advanceTimersByTimeAsync(2500); // one poll tick
    expect(api.fetchOrder).toHaveBeenCalledTimes(2);

    vi.mocked(api.fetchOrder).mockResolvedValue(customerOrder({ status: 'COMPLETED' }));
    await vi.advanceTimersByTimeAsync(2500); // tick → COMPLETED
    expect(api.fetchOrder).toHaveBeenCalledTimes(3);

    await vi.advanceTimersByTimeAsync(5000); // further ticks are skipped
    expect(api.fetchOrder).toHaveBeenCalledTimes(3);
  });

  it('stops polling after the component unmounts', async () => {
    vi.useFakeTimers();
    vi.mocked(api.fetchOrder).mockResolvedValue(customerOrder({ status: 'ORDERED' }));
    const { unmount } = await mount(OrderTrackingView, '/client/suivi/abc');
    await vi.waitFor(() => expect(api.fetchOrder).toHaveBeenCalledTimes(1));
    unmount();
    await vi.advanceTimersByTimeAsync(10000);
    expect(api.fetchOrder).toHaveBeenCalledTimes(1);
  });
});
