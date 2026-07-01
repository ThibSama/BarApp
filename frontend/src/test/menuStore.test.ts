import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useMenuStore } from '@/stores/menu';
import { ApiError } from '@/services/apiClient';
import { menuResponse } from './fixtures/catalog';

vi.mock('@/services/menuApi', () => ({ fetchMenu: vi.fn() }));
import * as menuApi from '@/services/menuApi';

describe('menu store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(menuApi.fetchMenu).mockReset();
  });

  it('loads the menu and flattens cocktails with their category', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(menuResponse());
    const store = useMenuStore();
    await store.load({ initial: true });
    expect(store.loaded).toBe(true);
    expect(store.cocktails).toHaveLength(3);
    expect(store.getCocktailById(101)?.categoryName).toBe('Classiques');
    expect(store.lastLoadedAt).not.toBeNull();
  });

  it('orders categories deterministically by displayOrder', async () => {
    const payload = menuResponse();
    payload.categories.reverse();
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(payload);
    const store = useMenuStore();
    await store.load({ initial: true });
    expect(store.categories.map((c) => c.id)).toEqual([1, 2, 3]);
  });

  it('deduplicates simultaneous loads into one request', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValue(menuResponse());
    const store = useMenuStore();
    await Promise.all([store.load({ initial: true }), store.load({ initial: true })]);
    expect(menuApi.fetchMenu).toHaveBeenCalledTimes(1);
  });

  it('surfaces an error and never falls back to fake data', async () => {
    vi.mocked(menuApi.fetchMenu).mockRejectedValueOnce(new ApiError({ message: 'x', status: 0, isNetworkError: true }));
    const store = useMenuStore();
    await store.load({ initial: true });
    expect(store.error).not.toBe('');
    expect(store.cocktails).toHaveLength(0);

    vi.mocked(menuApi.fetchMenu).mockResolvedValueOnce(menuResponse());
    await store.retry();
    expect(store.error).toBe('');
    expect(store.cocktails).toHaveLength(3);
  });

  it('retains the last menu on a transient refresh failure', async () => {
    vi.mocked(menuApi.fetchMenu).mockResolvedValueOnce(menuResponse());
    const store = useMenuStore();
    await store.load({ initial: true });
    vi.mocked(menuApi.fetchMenu).mockRejectedValueOnce(new Error('blip'));
    await store.load({ initial: false });
    expect(store.cocktails).toHaveLength(3);
    expect(store.error).not.toBe('');
  });
});
