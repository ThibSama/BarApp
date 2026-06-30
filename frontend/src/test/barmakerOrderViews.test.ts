import { fireEvent, render, screen } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import OrderDashboardView from '@/views/barmaker/OrderDashboardView.vue';
import BarmakerOrderDetailsView from '@/views/barmaker/BarmakerOrderDetailsView.vue';
import { useAuthStore } from '@/stores/auth';
import * as orderApi from '@/services/barmakerOrderApi';
import type { BarOrderDetail, BarOrderSummary } from '@/types/api';

vi.mock('@/services/barmakerOrderApi', () => ({
  fetchOrderSummaries: vi.fn(),
  fetchOrderDetail: vi.fn(),
  advanceOrderItem: vi.fn(),
}));

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

const summary: BarOrderSummary = {
  id: 'o1',
  publicCode: 'ABC234',
  status: 'IN_PROGRESS',
  totalAmount: 20,
  createdAt: '2026-06-30T10:00:00Z',
  completedAt: null,
  itemCount: 2,
  completedItemCount: 1,
};

const twoItemDetail: BarOrderDetail = {
  id: 'o1',
  publicCode: 'ABC234',
  status: 'ORDERED',
  totalAmount: 20,
  createdAt: '2026-06-30T10:00:00Z',
  completedAt: null,
  items: [
    { id: 'i1', sequenceNumber: 1, cocktailName: 'Mojito', size: 'M', unitPrice: 10, preparationStatus: 'PREPARATION_INGREDIENTS', completedAt: null },
    { id: 'i2', sequenceNumber: 2, cocktailName: 'Spritz', size: 'L', unitPrice: 10, preparationStatus: 'COMPLETED', completedAt: '2026-06-30T10:05:00Z' },
  ],
};

describe('barmaker order views', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(orderApi.fetchOrderSummaries).mockReset().mockResolvedValue([]);
    vi.mocked(orderApi.fetchOrderDetail).mockReset();
    vi.mocked(orderApi.advanceOrderItem).mockReset();
  });

  it('renders active summaries on the dashboard using publicCode', async () => {
    vi.mocked(orderApi.fetchOrderSummaries).mockImplementation((completed) =>
      Promise.resolve(completed ? [] : [summary]),
    );
    const r = await makeRouter('/bar/orders');
    render(OrderDashboardView, { global: { plugins: [r] } });

    expect(await screen.findByText('ABC234')).toBeTruthy();
    expect(screen.getByText('1/2 cocktail(s)')).toBeTruthy();
  });

  it('shows an empty state when there are no active orders', async () => {
    vi.mocked(orderApi.fetchOrderSummaries).mockResolvedValue([]);
    const r = await makeRouter('/bar/orders');
    render(OrderDashboardView, { global: { plugins: [r] } });

    expect(await screen.findByText('Aucune commande à traiter')).toBeTruthy();
  });

  it('loads detail directly, disables a completed cocktail, and advances exactly one step', async () => {
    const auth = useAuthStore();
    auth.accessToken = 'tok';
    auth.sessionValidated = true;

    vi.mocked(orderApi.fetchOrderDetail).mockResolvedValue(twoItemDetail);
    const advanced: BarOrderDetail = {
      ...twoItemDetail,
      status: 'IN_PROGRESS',
      items: [
        { ...twoItemDetail.items[0], preparationStatus: 'ASSEMBLY' },
        twoItemDetail.items[1],
      ],
    };
    vi.mocked(orderApi.advanceOrderItem).mockResolvedValue(advanced);

    const r = await makeRouter('/bar/orders/o1');
    render(BarmakerOrderDetailsView, { global: { plugins: [r] } });

    await screen.findByRole('heading', { name: 'ABC234' });

    const completedButton = screen.getByRole('button', { name: 'Cocktail terminé' }) as HTMLButtonElement;
    expect(completedButton.disabled).toBe(true);

    await fireEvent.click(screen.getByRole('button', { name: 'Commencer l’assemblage' }));
    expect(orderApi.advanceOrderItem).toHaveBeenCalledTimes(1);
    expect(orderApi.advanceOrderItem).toHaveBeenCalledWith('i1');
  });
});
