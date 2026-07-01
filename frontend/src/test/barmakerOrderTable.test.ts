import { render, screen, within } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import OrderDashboardView from '@/views/barmaker/OrderDashboardView.vue';
import BarmakerOrderDetailsView from '@/views/barmaker/BarmakerOrderDetailsView.vue';
import { useAuthStore } from '@/stores/auth';
import { barOrderDetail, barOrderSummary } from './fixtures/catalog';

vi.mock('@/services/barmakerOrderApi', () => ({
  fetchOrderSummaries: vi.fn(),
  fetchOrderDetail: vi.fn(),
  advanceOrderItem: vi.fn(),
}));
import * as orderApi from '@/services/barmakerOrderApi';

function makeRouter(path: string) {
  const instance = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/bar/orders', name: 'bar-orders', component: { template: '<div />' } },
      { path: '/bar/orders/:orderId', name: 'bar-order-details', component: { template: '<div />' } },
      { path: '/bar/login', name: 'bar-login', component: { template: '<div />' } },
    ],
  });
  return instance.push(path).then(() => instance);
}

describe('barmaker order table number + priority', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(orderApi.fetchOrderSummaries).mockReset().mockResolvedValue([]);
    vi.mocked(orderApi.fetchOrderDetail).mockReset();
  });

  it('shows the table number in the queue summary and orders ORDERED before IN_PROGRESS', async () => {
    const inProgressOld = barOrderSummary({ id: 'a', publicCode: 'OLD-IP', status: 'IN_PROGRESS', tableNumber: 3, createdAt: '2026-06-30T09:00:00Z' });
    const orderedNew = barOrderSummary({ id: 'b', publicCode: 'NEW-ORD', status: 'ORDERED', tableNumber: 8, createdAt: '2026-06-30T10:00:00Z' });
    vi.mocked(orderApi.fetchOrderSummaries).mockImplementation((completed) =>
      Promise.resolve(completed ? [] : [inProgressOld, orderedNew]),
    );
    const r = await makeRouter('/bar/orders');
    render(OrderDashboardView, { global: { plugins: [r] } });

    expect(await screen.findByText('NEW-ORD')).toBeTruthy();
    expect(screen.getByText('Table 8')).toBeTruthy();
    const codes = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
    // ORDERED (NEW-ORD) must precede IN_PROGRESS (OLD-IP) despite being newer.
    expect(codes.indexOf('NEW-ORD')).toBeLessThan(codes.indexOf('OLD-IP'));
  });

  it('shows the table number and payment method in the order detail', async () => {
    const auth = useAuthStore();
    auth.accessToken = 'tok';
    auth.sessionValidated = true;
    vi.mocked(orderApi.fetchOrderDetail).mockResolvedValue(barOrderDetail({ tableNumber: 42, paymentMethod: 'APPLE_PAY' }));
    const r = await makeRouter('/bar/orders/o1');
    render(BarmakerOrderDetailsView, { global: { plugins: [r] } });

    const summary = (await screen.findByText('Résumé')).closest('aside') as HTMLElement;
    expect(within(summary).getByText('42')).toBeTruthy();
    expect(within(summary).getByText('Apple Pay')).toBeTruthy();
  });
});
