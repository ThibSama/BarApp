import { createRouter, createWebHistory } from 'vue-router';
import NotFoundView from '@/views/shared/NotFoundView.vue';
import ClientLayout from '@/layouts/ClientLayout.vue';
import BarmakerLayout from '@/layouts/BarmakerLayout.vue';
import MenuView from '@/views/client/MenuView.vue';
import CocktailDetailsView from '@/views/client/CocktailDetailsView.vue';
import CartView from '@/views/client/CartView.vue';
import OrderConfirmationView from '@/views/client/OrderConfirmationView.vue';
import OrderTrackingView from '@/views/client/OrderTrackingView.vue';
import OrderDashboardView from '@/views/barmaker/OrderDashboardView.vue';
import BarmakerOrderDetailsView from '@/views/barmaker/BarmakerOrderDetailsView.vue';
import CategoryManagementView from '@/views/barmaker/CategoryManagementView.vue';
import CocktailManagementView from '@/views/barmaker/CocktailManagementView.vue';
import CocktailFormView from '@/views/barmaker/CocktailFormView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/client/menu' },
    { path: '/client', component: ClientLayout, children: [
      { path: '', redirect: '/client/menu' },
      { path: 'menu', name: 'client-menu', component: MenuView },
      { path: 'cocktails/:id', name: 'client-cocktail-details', component: CocktailDetailsView },
      { path: 'panier', name: 'client-cart', component: CartView },
      { path: 'confirmation/:orderId', name: 'client-order-confirmation', component: OrderConfirmationView },
      { path: 'suivi/:orderId', name: 'client-order-tracking', component: OrderTrackingView },
    ]},
    { path: '/barmaker', component: BarmakerLayout, children: [
      { path: '', redirect: '/barmaker/commandes' },
      { path: 'commandes', name: 'barmaker-orders', component: OrderDashboardView },
      { path: 'commandes/:orderId', name: 'barmaker-order-details', component: BarmakerOrderDetailsView },
      { path: 'categories', name: 'barmaker-categories', component: CategoryManagementView },
      { path: 'cocktails', name: 'barmaker-cocktails', component: CocktailManagementView },
      { path: 'cocktails/nouveau', name: 'barmaker-cocktail-new', component: CocktailFormView },
      { path: 'cocktails/:cocktailId/modifier', name: 'barmaker-cocktail-edit', component: CocktailFormView },
    ]},
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
  ],
});

export default router;
