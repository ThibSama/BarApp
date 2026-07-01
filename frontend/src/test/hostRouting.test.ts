import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { buildRoutes, resolveHostMode } from '@/router/routes';
import { createAppRouter } from '@/router';
import { useAuthStore } from '@/stores/auth';

// Barmaker guards may touch the auth store which can call the API; stub fetch so
// any stray call is a harmless 401 and never hits the network.
vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ status: 401, ok: false, text: async () => '' }));

function authenticate(): void {
  const auth = useAuthStore();
  auth.accessToken = 'tok';
  auth.sessionValidated = true;
}

describe('resolveHostMode', () => {
  it('maps the two canonical hostnames and treats everything else as legacy', () => {
    expect(resolveHostMode('client.localhost')).toBe('client');
    expect(resolveHostMode('barmaker.localhost')).toBe('barmaker');
    expect(resolveHostMode('CLIENT.localhost')).toBe('client'); // case-insensitive
    expect(resolveHostMode('barmaker.localhost:5173')).toBe('barmaker'); // tolerates :port
    expect(resolveHostMode('localhost')).toBe('legacy');
    expect(resolveHostMode('127.0.0.1')).toBe('legacy');
    expect(resolveHostMode('192.168.1.50')).toBe('legacy');
    expect(resolveHostMode('')).toBe('legacy');
  });
});

describe('buildRoutes: client.localhost', () => {
  const router = createAppRouter('client.localhost');

  it('resolves the root to the client menu (not a barmaker page)', () => {
    const resolved = router.resolve('/');
    expect(resolved.name).toBe('client-menu');
  });

  it('generates clean named client paths', () => {
    expect(router.resolve({ name: 'client-cart' }).path).toBe('/panier');
    expect(router.resolve({ name: 'client-cocktail-details', params: { id: '5' } }).path).toBe('/cocktails/5');
    expect(router.resolve({ name: 'client-order-confirmation', params: { orderId: 'abc' } }).path).toBe('/confirmation/abc');
    expect(router.resolve({ name: 'client-current-order' }).path).toBe('/suivi');
    expect(router.resolve({ name: 'client-order-tracking', params: { orderId: 'abc' } }).path).toBe('/suivi/abc');
  });

  it('supports deep-link paths directly', () => {
    expect(router.resolve('/panier').name).toBe('client-cart');
    expect(router.resolve('/suivi/xyz').name).toBe('client-order-tracking');
  });

  it('does not expose barmaker pages as real routes', () => {
    expect(buildRoutes('client.localhost').some((r) => r.name === 'bar-orders')).toBe(false);
  });
});

describe('buildRoutes: barmaker.localhost', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('generates clean named barmaker paths', () => {
    const router = createAppRouter('barmaker.localhost');
    expect(router.resolve({ name: 'bar-login' }).path).toBe('/login');
    expect(router.resolve({ name: 'bar-orders' }).path).toBe('/orders');
    expect(router.resolve({ name: 'bar-order-details', params: { orderId: 'abc' } }).path).toBe('/orders/abc');
    expect(router.resolve({ name: 'bar-categories' }).path).toBe('/categories');
    expect(router.resolve({ name: 'bar-cocktails' }).path).toBe('/cocktails');
  });

  it('sends an anonymous visitor from the root to /login', async () => {
    const router = createAppRouter('barmaker.localhost');
    await router.push('/');
    await router.isReady();
    expect(router.currentRoute.value.name).toBe('bar-login');
    expect(router.currentRoute.value.path).toBe('/login');
  });

  it('sends an authenticated barmaker from the root to /orders', async () => {
    const router = createAppRouter('barmaker.localhost');
    authenticate();
    await router.push('/');
    await router.isReady();
    expect(router.currentRoute.value.name).toBe('bar-orders');
    expect(router.currentRoute.value.path).toBe('/orders');
  });

  it('protects deep links and preserves the destination', async () => {
    const router = createAppRouter('barmaker.localhost');
    await router.push('/categories');
    expect(router.currentRoute.value.name).toBe('bar-login');
    expect(router.currentRoute.value.query.redirect).toBe('/categories');
  });

  it('supports deep-link paths directly', () => {
    const router = createAppRouter('barmaker.localhost');
    expect(router.resolve('/orders').name).toBe('bar-orders');
    expect(router.resolve('/orders/abc').name).toBe('bar-order-details');
  });
});

describe('buildRoutes: legacy hostname', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('keeps the historical client and barmaker paths', async () => {
    const router = createAppRouter('localhost');
    await router.push('/');
    await router.isReady();
    expect(router.currentRoute.value.path).toBe('/client/menu');
    expect(router.resolve('/client/menu').name).toBe('client-menu');
    expect(router.resolve('/bar/login').name).toBe('bar-login');
    expect(router.resolve({ name: 'bar-orders' }).path).toBe('/bar/orders');
  });

  it('keeps /bar/orders protected', async () => {
    const router = createAppRouter('192.168.1.50');
    await router.push('/bar/orders');
    expect(router.currentRoute.value.name).toBe('bar-login');
    expect(router.currentRoute.value.query.redirect).toBe('/bar/orders');
  });

  it('keeps the /barmaker/** compatibility redirects', async () => {
    const router = createAppRouter('localhost');
    authenticate();
    await router.push('/barmaker/commandes/abc-123');
    expect(router.currentRoute.value.path).toBe('/bar/orders/abc-123');
  });
});
