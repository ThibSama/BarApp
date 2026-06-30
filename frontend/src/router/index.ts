import { createRouter, createWebHistory } from 'vue-router';
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
import { useAuthStore } from '@/stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/client/menu' },

    // --- Customer flow (public, still mock-backed) ---
    { path: '/client', component: ClientLayout, children: [
      { path: '', redirect: '/client/menu' },
      { path: 'menu', name: 'client-menu', component: MenuView },
      { path: 'cocktails/:id', name: 'client-cocktail-details', component: CocktailDetailsView },
      { path: 'panier', name: 'client-cart', component: CartView },
      { path: 'confirmation/:orderId', name: 'client-order-confirmation', component: OrderConfirmationView },
      { path: 'suivi', name: 'client-current-order', component: OrderTrackingView },
      { path: 'suivi/:orderId', name: 'client-order-tracking', component: OrderTrackingView },
    ]},

    // --- Barmaker login (public, outside the authenticated shell) ---
    { path: '/bar/login', name: 'bar-login', component: BarmakerLoginView },

    // --- Barmaker workspace (all children require authentication) ---
    { path: '/bar', component: BarmakerLayout, meta: { requiresAuth: true }, children: [
      { path: '', redirect: '/bar/orders' },
      { path: 'orders', name: 'bar-orders', component: OrderDashboardView, meta: { requiresAuth: true } },
      { path: 'orders/:orderId', name: 'bar-order-details', component: BarmakerOrderDetailsView, meta: { requiresAuth: true } },
      { path: 'categories', name: 'bar-categories', component: CategoryManagementView, meta: { requiresAuth: true } },
      { path: 'cocktails', name: 'bar-cocktails', component: CocktailManagementView, meta: { requiresAuth: true } },
      { path: 'cocktails/new', name: 'bar-cocktail-new', redirect: { name: 'bar-cocktails', query: { modal: 'create' } } },
      { path: 'cocktails/:cocktailId/edit', name: 'bar-cocktail-edit', redirect: (to) => ({ name: 'bar-cocktails', query: { modal: 'edit', cocktailId: String(to.params.cocktailId) } }) },
    ]},

    // --- Backward-compatibility redirects from the old /barmaker/... paths ---
    { path: '/barmaker', redirect: '/bar/orders' },
    { path: '/barmaker/commandes', redirect: '/bar/orders' },
    { path: '/barmaker/commandes/:orderId', redirect: (to) => `/bar/orders/${to.params.orderId}` },
    { path: '/barmaker/categories', redirect: '/bar/categories' },
    { path: '/barmaker/cocktails', redirect: '/bar/cocktails' },
    { path: '/barmaker/cocktails/nouveau', redirect: '/bar/cocktails/new' },
    { path: '/barmaker/cocktails/:cocktailId/modifier', redirect: (to) => `/bar/cocktails/${to.params.cocktailId}/edit` },

    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
  ],
});

router.beforeEach(async (to) => {
  // Public client routes never touch the auth store (so no Pinia dependency).
  if (to.name !== 'bar-login' && !to.meta.requiresAuth) return true;

  const auth = useAuthStore();

  // Already-authenticated barmaker should not see the login screen.
  if (to.name === 'bar-login') {
    const valid = auth.isAuthenticated || (Boolean(auth.accessToken) && (await auth.ensureSession()));
    return valid ? { name: 'bar-orders' } : true;
  }

  if (to.meta.requiresAuth) {
    const valid = auth.isAuthenticated || (Boolean(auth.accessToken) && (await auth.ensureSession()));
    if (valid) return true;
    // Preserve the intended internal destination as a safe redirect query.
    return { name: 'bar-login', query: { redirect: to.fullPath } };
  }

  return true;
});

export default router;
