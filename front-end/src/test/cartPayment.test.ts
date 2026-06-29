import { fireEvent, render, screen } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import CartView from '@/views/client/CartView.vue';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/orders';

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', redirect: '/client/menu' },
      { path: '/client/menu', component: { template: '<div />' } },
      { path: '/client/confirmation/:orderId', component: { template: '<div />' } },
    ],
  });
}

describe('cart payment flow', () => {
  beforeEach(() => {
    const pinia = createPinia();
    setActivePinia(pinia);
  });

  it('does not allow confirmation without a selected payment method', async () => {
    const router = createTestRouter();
    const cart = useCartStore();
    cart.addItem('mojito', 'M', 1);
    render(CartView, { global: { plugins: [router] } });
    const button = screen.getByRole('button', { name: 'Confirmer la commande' }) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(screen.getByText('Sélectionnez un mode de paiement pour valider la commande.')).toBeTruthy();
  });

  it('allows confirmation after selecting a payment method', async () => {
    const router = createTestRouter();
    const cart = useCartStore();
    const orders = useOrderStore();
    cart.addItem('mojito', 'M', 1);
    render(CartView, { global: { plugins: [router] } });
    await fireEvent.click(screen.getByDisplayValue('apple_pay'));
    const button = screen.getByRole('button', { name: 'Confirmer la commande' }) as HTMLButtonElement;
    expect(button.disabled).toBe(false);
    await fireEvent.click(button);
    expect(orders.orders[0].paymentMethod).toBe('apple_pay');
    expect(cart.items).toHaveLength(0);
  });
});
