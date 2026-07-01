import type { RouteRecordRaw } from 'vue-router';
import NotFoundView from '@/views/shared/NotFoundView.vue';
import ClientLayout from '@/layouts/ClientLayout.vue';
import BarmakerLayout from '@/layouts/BarmakerLayout.vue';
import MenuView from '@/views/client/MenuView.vue';
import CocktailDetailsView from '@/views/client/CocktailDetailsView.vue';
import CartView from '@/views/client/CartView.vue';
import OrderConfirmationView from '@/views/client/OrderConfirmationView.vue';
import OrderTrackingView from '@/views/client/OrderTrackingView.vue';
import BarmakerLoginView from '@/views/barmaker/BarmakerLoginView.vue';
import OrderDashboardView from '@/views/barmaker/OrderDashboardView.vue';
import BarmakerOrderDetailsView from '@/views/barmaker/BarmakerOrderDetailsView.vue';
import CategoryManagementView from '@/views/barmaker/CategoryManagementView.vue';
import CocktailManagementView from '@/views/barmaker/CocktailManagementView.vue';
import UserManagementView from '@/views/barmaker/UserManagementView.vue';

/**
 * Typed route metadata shared by every mode. `requiresAuth` gates the barmaker
 * workspace; `requiresManager` additionally restricts a route to managers (it
 * implies authentication). The router guard is the single enforcement point.
 */
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    requiresManager?: boolean;
  }
}

/**
 * Role-specific local hostnames. Opening the app on one of these produces clean,
 * role-scoped URLs; every other hostname (localhost, 127.0.0.1, a LAN IP, an
 * unknown host) keeps the historical `/client/**` and `/bar/**` layout for full
 * backward compatibility and two-device LAN demos.
 */
export const CLIENT_HOSTNAME = 'client.localhost';
export const BARMAKER_HOSTNAME = 'barmaker.localhost';

export type HostMode = 'client' | 'barmaker' | 'legacy';

/**
 * Map a raw hostname to a routing mode. Only the two exact `.localhost`
 * hostnames switch to canonical mode; anything else stays legacy so LAN/IP
 * access is never forced onto a `.localhost` host. Case-insensitive and
 * tolerant of a trailing `:port`.
 */
export function resolveHostMode(hostname: string | null | undefined): HostMode {
  const host = (hostname ?? '').toLowerCase().split(':')[0];
  if (host === CLIENT_HOSTNAME) return 'client';
  if (host === BARMAKER_HOSTNAME) return 'barmaker';
  return 'legacy';
}

const notFoundRoute: RouteRecordRaw = {
  path: '/:pathMatch(.*)*',
  name: 'not-found',
  component: NotFoundView,
};

/**
 * Canonical client routes served on `client.localhost`. Route names are kept
 * identical to every other mode so components navigate by name and never need
 * to know the active hostname.
 */
function clientCanonicalRoutes(): RouteRecordRaw[] {
  return [
    {
      path: '/',
      component: ClientLayout,
      children: [
        { path: '', name: 'client-menu', component: MenuView },
        { path: 'cocktails/:id', name: 'client-cocktail-details', component: CocktailDetailsView },
        { path: 'panier', name: 'client-cart', component: CartView },
        { path: 'confirmation/:orderId', name: 'client-order-confirmation', component: OrderConfirmationView },
        { path: 'suivi', name: 'client-current-order', component: OrderTrackingView },
        { path: 'suivi/:orderId', name: 'client-order-tracking', component: OrderTrackingView },
      ],
    },
    notFoundRoute,
  ];
}

/**
 * Canonical barmaker routes served on `barmaker.localhost`. The root redirects
 * to the orders queue; the guard then sends an anonymous visitor to `/login`
 * and an authenticated one straight to `/orders`.
 */
function barmakerCanonicalRoutes(): RouteRecordRaw[] {
  return [
    { path: '/login', name: 'bar-login', component: BarmakerLoginView },
    {
      path: '/',
      component: BarmakerLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: { name: 'bar-orders' } },
        { path: 'orders', name: 'bar-orders', component: OrderDashboardView, meta: { requiresAuth: true } },
        { path: 'orders/:orderId', name: 'bar-order-details', component: BarmakerOrderDetailsView, meta: { requiresAuth: true } },
        { path: 'categories', name: 'bar-categories', component: CategoryManagementView, meta: { requiresAuth: true } },
        { path: 'cocktails', name: 'bar-cocktails', component: CocktailManagementView, meta: { requiresAuth: true } },
        { path: 'cocktails/new', name: 'bar-cocktail-new', redirect: { name: 'bar-cocktails', query: { modal: 'create' } } },
        {
          path: 'cocktails/:cocktailId/edit',
          name: 'bar-cocktail-edit',
          redirect: (to) => ({ name: 'bar-cocktails', query: { modal: 'edit', cocktailId: String(to.params.cocktailId) } }),
        },
        { path: 'users', name: 'bar-users', component: UserManagementView, meta: { requiresAuth: true, requiresManager: true } },
      ],
    },
    notFoundRoute,
  ];
}

/**
 * Legacy routes for `localhost`, `127.0.0.1`, LAN IPs and any unknown host.
 * These are the exact historical paths and redirects; nothing here changes so
 * the currently validated URLs and the `/barmaker/**` compatibility redirects
 * keep working unchanged.
 */
function legacyRoutes(): RouteRecordRaw[] {
  return [
    { path: '/', redirect: '/client/menu' },

    // --- Customer flow (public, backed by the real /api/menu and /api/orders) ---
    {
      path: '/client',
      component: ClientLayout,
      children: [
        { path: '', redirect: '/client/menu' },
        { path: 'menu', name: 'client-menu', component: MenuView },
        { path: 'cocktails/:id', name: 'client-cocktail-details', component: CocktailDetailsView },
        { path: 'panier', name: 'client-cart', component: CartView },
        { path: 'confirmation/:orderId', name: 'client-order-confirmation', component: OrderConfirmationView },
        { path: 'suivi', name: 'client-current-order', component: OrderTrackingView },
        { path: 'suivi/:orderId', name: 'client-order-tracking', component: OrderTrackingView },
      ],
    },

    // --- Barmaker login (public, outside the authenticated shell) ---
    { path: '/bar/login', name: 'bar-login', component: BarmakerLoginView },

    // --- Barmaker workspace (all children require authentication) ---
    {
      path: '/bar',
      component: BarmakerLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/bar/orders' },
        { path: 'orders', name: 'bar-orders', component: OrderDashboardView, meta: { requiresAuth: true } },
        { path: 'orders/:orderId', name: 'bar-order-details', component: BarmakerOrderDetailsView, meta: { requiresAuth: true } },
        { path: 'categories', name: 'bar-categories', component: CategoryManagementView, meta: { requiresAuth: true } },
        { path: 'cocktails', name: 'bar-cocktails', component: CocktailManagementView, meta: { requiresAuth: true } },
        { path: 'cocktails/new', name: 'bar-cocktail-new', redirect: { name: 'bar-cocktails', query: { modal: 'create' } } },
        { path: 'cocktails/:cocktailId/edit', name: 'bar-cocktail-edit', redirect: (to) => `/bar/cocktails?modal=edit&cocktailId=${encodeURIComponent(String(to.params.cocktailId))}` },
        { path: 'users', name: 'bar-users', component: UserManagementView, meta: { requiresAuth: true, requiresManager: true } },
      ],
    },

    // --- Backward-compatibility redirects from the old /barmaker/... paths ---
    { path: '/barmaker', redirect: '/bar/orders' },
    { path: '/barmaker/commandes', redirect: '/bar/orders' },
    { path: '/barmaker/commandes/:orderId', redirect: (to) => `/bar/orders/${to.params.orderId}` },
    { path: '/barmaker/categories', redirect: '/bar/categories' },
    { path: '/barmaker/cocktails', redirect: '/bar/cocktails' },
    { path: '/barmaker/cocktails/nouveau', redirect: '/bar/cocktails/new' },
    { path: '/barmaker/cocktails/:cocktailId/modifier', redirect: (to) => `/bar/cocktails/${to.params.cocktailId}/edit` },

    notFoundRoute,
  ];
}

/**
 * Build the route table for a hostname. Pure and side-effect free so it can be
 * unit-tested against any hostname without a browser.
 */
export function buildRoutes(hostname: string | null | undefined): RouteRecordRaw[] {
  switch (resolveHostMode(hostname)) {
    case 'client':
      return clientCanonicalRoutes();
    case 'barmaker':
      return barmakerCanonicalRoutes();
    default:
      return legacyRoutes();
  }
}
