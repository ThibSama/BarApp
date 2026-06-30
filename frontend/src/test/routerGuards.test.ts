import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import router from '@/router';
import { useAuthStore } from '@/stores/auth';

// The barmaker views pull in stores that may call the API on mount; the guard
// tests only resolve navigation (no component mount), but we stub fetch so any
// stray call is harmless and never hits the network.
vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ status: 401, ok: false, text: async () => '' }));

function authenticate(): void {
  const auth = useAuthStore();
  auth.accessToken = 'tok';
  auth.sessionValidated = true;
}

describe('router guards', () => {
  beforeEach(async () => {
    setActivePinia(createPinia());
    await router.replace('/client/menu');
  });

  it('redirects an anonymous visitor from /bar/orders to /bar/login, preserving the destination', async () => {
    await router.push('/bar/orders');
    await router.isReady();
    expect(router.currentRoute.value.name).toBe('bar-login');
    expect(router.currentRoute.value.query.redirect).toBe('/bar/orders');
  });

  it('lets an authenticated barmaker reach /bar/orders', async () => {
    authenticate();
    await router.push('/bar/orders');
    expect(router.currentRoute.value.name).toBe('bar-orders');
  });

  it('redirects an authenticated barmaker away from /bar/login', async () => {
    authenticate();
    await router.push('/bar/login');
    expect(router.currentRoute.value.name).toBe('bar-orders');
  });

  it('redirects old /barmaker/... paths to the canonical /bar/... paths', async () => {
    authenticate();
    await router.push('/barmaker/commandes/abc-123');
    expect(router.currentRoute.value.path).toBe('/bar/orders/abc-123');
  });

  it('keeps client routes public', async () => {
    await router.push('/client/menu');
    expect(router.currentRoute.value.name).toBe('client-menu');
  });
});
