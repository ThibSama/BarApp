import { fireEvent, render, screen, within } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { describe, expect, it } from 'vitest';
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import ClientLayout from '@/layouts/ClientLayout.vue';
import BarmakerLayout from '@/layouts/BarmakerLayout.vue';
import { useAuthStore } from '@/stores/auth';
import type { AuthenticatedUser } from '@/types/api';

const stub = { template: '<div />' };

function createClientRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/client/menu', name: 'client-menu', component: stub },
      { path: '/client/panier', name: 'client-cart', component: stub },
      { path: '/client/suivi', name: 'client-current-order', component: stub },
    ],
  });
}

// Barmaker nav links navigate by name; the path they render depends only on the
// route table, so the same layout produces clean paths in canonical mode and
// legacy paths in legacy mode.
function createBarmakerRouter(paths: Record<string, string>) {
  const routes: RouteRecordRaw[] = [
    { path: paths['bar-orders'], name: 'bar-orders', component: stub },
    { path: paths['bar-categories'], name: 'bar-categories', component: stub },
    { path: paths['bar-cocktails'], name: 'bar-cocktails', component: stub },
    { path: paths['bar-users'] ?? '/users', name: 'bar-users', component: stub },
    { path: paths['bar-login'], name: 'bar-login', component: stub },
  ];
  return createRouter({ history: createWebHistory(), routes });
}

const managerUser: AuthenticatedUser = {
  id: 2,
  username: 'manager',
  displayName: 'Manager du bar',
  role: 'MANAGER',
};

const barmakerUser: AuthenticatedUser = {
  id: 1,
  username: 'barmaker',
  displayName: 'Barman principal',
  role: 'BARMAKER',
};

// Build a pinia whose auth store already holds the given user, so BarmakerLayout
// renders the manager-only navigation exactly as it would after a real login.
function piniaWithUser(user: AuthenticatedUser | null) {
  const pinia = createPinia();
  setActivePinia(pinia);
  const auth = useAuthStore();
  auth.user = user;
  return pinia;
}

describe('client header', () => {
  it('exposes only the public client links', async () => {
    const router = createClientRouter();
    await router.push('/client/menu');
    render(ClientLayout, { global: { plugins: [createPinia(), router] } });
    const navigation = screen.getByRole('navigation', { name: 'Navigation client' });
    expect(within(navigation).getByRole('link', { name: 'Carte des cocktails' })).toBeTruthy();
    expect(within(navigation).getByRole('link', { name: /Panier/ })).toBeTruthy();
    expect(within(navigation).getByRole('link', { name: 'Ma commande' })).toBeTruthy();
    expect(within(navigation).queryByText('Espace barmaker')).toBeNull();
    expect(within(navigation).queryByText('Espace client')).toBeNull();
    expect(within(navigation).queryByText(/compte|profil|connexion|inscription/i)).toBeNull();
  });

  it('shows the approved brand and subtitle in the customer sidebar', async () => {
    const router = createClientRouter();
    await router.push('/client/menu');
    render(ClientLayout, { global: { plugins: [createPinia(), router] } });
    const sidebar = screen.getByRole('complementary', { name: 'Navigation client bureau' });
    expect(within(sidebar).getByText(/LE BAR.APP/)).toBeTruthy();
    expect(within(sidebar).getByText('COCKTAILS & GOOD VIBES')).toBeTruthy();
    expect(within(sidebar).queryByText(/espace barmaker/i)).toBeNull();
  });
});

describe('barmaker sidebar navigation', () => {
  it('renders the clean canonical paths on barmaker.localhost', async () => {
    const router = createBarmakerRouter({
      'bar-orders': '/orders',
      'bar-categories': '/categories',
      'bar-cocktails': '/cocktails',
      'bar-login': '/login',
    });
    await router.push('/orders');
    render(BarmakerLayout, { global: { plugins: [createPinia(), router] } });
    const navigation = screen.getByRole('navigation', { name: 'Navigation barmaker' });
    expect(within(navigation).getByRole('link', { name: 'Commandes' }).getAttribute('href')).toBe('/orders');
    expect(within(navigation).getByRole('link', { name: 'Catégories' }).getAttribute('href')).toBe('/categories');
    expect(within(navigation).getByRole('link', { name: 'Cocktails' }).getAttribute('href')).toBe('/cocktails');
  });

  it('keeps the legacy /bar/** paths in legacy mode', async () => {
    const router = createBarmakerRouter({
      'bar-orders': '/bar/orders',
      'bar-categories': '/bar/categories',
      'bar-cocktails': '/bar/cocktails',
      'bar-login': '/bar/login',
    });
    await router.push('/bar/orders');
    render(BarmakerLayout, { global: { plugins: [createPinia(), router] } });
    const navigation = screen.getByRole('navigation', { name: 'Navigation barmaker' });
    expect(within(navigation).getByRole('link', { name: 'Commandes' }).getAttribute('href')).toBe('/bar/orders');
    expect(within(navigation).getByRole('link', { name: 'Cocktails' }).getAttribute('href')).toBe('/bar/cocktails');
  });
});

describe('barmaker staff navigation (Équipe)', () => {
  const paths = {
    'bar-orders': '/orders',
    'bar-categories': '/categories',
    'bar-cocktails': '/cocktails',
    'bar-users': '/users',
    'bar-login': '/login',
  };

  it('shows Équipe in the desktop sidebar for a manager', async () => {
    const router = createBarmakerRouter(paths);
    await router.push('/orders');
    render(BarmakerLayout, { global: { plugins: [piniaWithUser(managerUser), router] } });
    const navigation = screen.getByRole('navigation', { name: 'Navigation barmaker' });
    const link = within(navigation).getByRole('link', { name: 'Équipe' });
    expect(link.getAttribute('href')).toBe('/users');
  });

  it('shows Équipe in the mobile drawer for a manager', async () => {
    const router = createBarmakerRouter(paths);
    await router.push('/orders');
    render(BarmakerLayout, { global: { plugins: [piniaWithUser(managerUser), router] } });
    await fireEvent.click(screen.getByRole('button', { name: 'Ouvrir la navigation barmaker' }));
    const drawer = screen.getByRole('navigation', { name: 'Navigation barmaker mobile' });
    expect(within(drawer).getByRole('link', { name: 'Équipe' })).toBeTruthy();
  });

  it('never shows Équipe for a regular barmaker (desktop or mobile)', async () => {
    const router = createBarmakerRouter(paths);
    await router.push('/orders');
    render(BarmakerLayout, { global: { plugins: [piniaWithUser(barmakerUser), router] } });
    const navigation = screen.getByRole('navigation', { name: 'Navigation barmaker' });
    expect(within(navigation).queryByRole('link', { name: 'Équipe' })).toBeNull();

    await fireEvent.click(screen.getByRole('button', { name: 'Ouvrir la navigation barmaker' }));
    const drawer = screen.getByRole('navigation', { name: 'Navigation barmaker mobile' });
    expect(within(drawer).queryByRole('link', { name: 'Équipe' })).toBeNull();
  });
});
