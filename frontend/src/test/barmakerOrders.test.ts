import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { ApiError } from '@/services/apiClient';
import { useBarmakerOrderStore } from '@/stores/barmakerOrders';
import { usePolling } from '@/composables/usePolling';
import * as orderApi from '@/services/barmakerOrderApi';
import type { BarOrderDetail, BarOrderSummary } from '@/types/api';

vi.mock('@/services/barmakerOrderApi', () => ({
  fetchOrderSummaries: vi.fn(),
  fetchOrderDetail: vi.fn(),
  advanceOrderItem: vi.fn(),
}));

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

const detail: BarOrderDetail = {
  id: 'o1',
  publicCode: 'ABC234',
  status: 'ORDERED',
  totalAmount: 20,
  createdAt: '2026-06-30T10:00:00Z',
  completedAt: null,
  items: [
    { id: 'i1', sequenceNumber: 1, cocktailName: 'Mojito', size: 'M', unitPrice: 10, preparationStatus: 'PREPARATION_INGREDIENTS', completedAt: null },
  ],
};

describe('barmaker order store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(orderApi.fetchOrderSummaries).mockReset();
    vi.mocked(orderApi.fetchOrderDetail).mockReset();
    vi.mocked(orderApi.advanceOrderItem).mockReset();
  });

  it('loads active summaries', async () => {
    vi.mocked(orderApi.fetchOrderSummaries).mockResolvedValue([summary]);
    const store = useBarmakerOrderStore();

    await store.loadActive({ initial: true });

    expect(store.activeSummaries).toEqual([summary]);
    expect(store.listError).toBe('');
    expect(store.lastRefreshedAt).not.toBeNull();
  });

  it('represents an empty active queue', async () => {
    vi.mocked(orderApi.fetchOrderSummaries).mockResolvedValue([]);
    const store = useBarmakerOrderStore();

    await store.loadActive({ initial: true });

    expect(store.activeSummaries).toEqual([]);
  });

  it('does not issue overlapping concurrent active requests', async () => {
    let resolve: (value: BarOrderSummary[]) => void = () => {};
    const pending = new Promise<BarOrderSummary[]>((r) => { resolve = r; });
    vi.mocked(orderApi.fetchOrderSummaries).mockReturnValue(pending);
    const store = useBarmakerOrderStore();

    const first = store.loadActive();
    const second = store.loadActive(); // must be skipped while one is in flight

    expect(orderApi.fetchOrderSummaries).toHaveBeenCalledTimes(1);
    resolve([summary]);
    await Promise.all([first, second]);
    expect(store.activeSummaries).toEqual([summary]);
  });

  it('loads order detail independently', async () => {
    vi.mocked(orderApi.fetchOrderDetail).mockResolvedValue(detail);
    const store = useBarmakerOrderStore();

    await store.loadDetail('o1', { initial: true });

    expect(store.detail).toEqual(detail);
    expect(store.detailNotFound).toBe(false);
  });

  it('flags a hard not-found on initial detail load', async () => {
    vi.mocked(orderApi.fetchOrderDetail).mockRejectedValue(
      new ApiError({ message: 'introuvable', status: 404, code: 'ORDER_NOT_FOUND' }),
    );
    const store = useBarmakerOrderStore();

    await store.loadDetail('missing', { initial: true });

    expect(store.detail).toBeNull();
    expect(store.detailNotFound).toBe(true);
  });

  it('advances exactly one item and replaces the detail with the backend order', async () => {
    const advanced: BarOrderDetail = {
      ...detail,
      status: 'IN_PROGRESS',
      items: [{ ...detail.items[0], preparationStatus: 'ASSEMBLY' }],
    };
    vi.mocked(orderApi.advanceOrderItem).mockResolvedValue(advanced);
    vi.mocked(orderApi.fetchOrderSummaries).mockResolvedValue([summary]);
    const store = useBarmakerOrderStore();

    const error = await store.advance('i1');

    expect(error).toBeNull();
    expect(orderApi.advanceOrderItem).toHaveBeenCalledTimes(1);
    expect(store.detail).toEqual(advanced);
    expect(orderApi.fetchOrderSummaries).toHaveBeenCalledWith(false);
  });

  it('refreshes the detail and returns a message on a 409 transition conflict', async () => {
    vi.mocked(orderApi.fetchOrderDetail).mockResolvedValue(detail);
    const store = useBarmakerOrderStore();
    await store.loadDetail('o1', { initial: true });

    vi.mocked(orderApi.advanceOrderItem).mockRejectedValue(
      new ApiError({ message: 'conflit', status: 409, code: 'INVALID_PREPARATION_TRANSITION' }),
    );
    vi.mocked(orderApi.fetchOrderDetail).mockClear();

    const error = await store.advance('i1');

    expect(error).toContain('déjà');
    // a fresh detail load is triggered to reflect the real backend state
    expect(orderApi.fetchOrderDetail).toHaveBeenCalledWith('o1');
  });

  it('retains visible summaries when a background refresh fails', async () => {
    vi.mocked(orderApi.fetchOrderSummaries).mockResolvedValueOnce([summary]);
    const store = useBarmakerOrderStore();
    await store.loadActive({ initial: true });

    vi.mocked(orderApi.fetchOrderSummaries).mockRejectedValueOnce(
      new ApiError({ message: 'réseau', status: 0, isNetworkError: true }),
    );
    await store.loadActive();

    expect(store.activeSummaries).toEqual([summary]);
    expect(store.listError).toBeTruthy();
  });
});

describe('usePolling', () => {
  it('polls on an interval and stops on unmount', () => {
    vi.useFakeTimers();
    const task = vi.fn();
    const Comp = defineComponent({
      setup() {
        usePolling(task, 1000);
        return () => h('div');
      },
    });

    const wrapper = mount(Comp);
    vi.advanceTimersByTime(3000);
    expect(task).toHaveBeenCalledTimes(3);

    wrapper.unmount();
    vi.advanceTimersByTime(3000);
    expect(task).toHaveBeenCalledTimes(3); // no further ticks after unmount

    vi.useRealTimers();
  });
});
