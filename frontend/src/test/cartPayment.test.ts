import { fireEvent, render, screen, waitFor } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import CartView from '@/views/client/CartView.vue';
import { useCartStore } from '@/stores/cart';
import { ApiError } from '@/services/apiClient';
import { customerOrder } from './fixtures/catalog';

vi.mock('@/services/customerOrderApi', () => ({
  createOrder: vi.fn(),
  fetchOrder: vi.fn(),
}));
import * as api from '@/services/customerOrderApi';

function makeRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', redirect: '/client/menu' },
      { path: '/client/menu', name: 'client-menu', component: { template: '<div />' } },
      { path: '/client/panier', name: 'client-cart', component: CartView },
      { path: '/client/confirmation/:orderId', name: 'client-order-confirmation', component: { template: '<div>Confirmation</div>' } },
    ],
  });
}

async function setup() {
  const router = makeRouter();
  await router.push('/client/panier');
  await router.isReady();
  const cart = useCartStore();
  cart.addItem({ cocktailId: 101, name: 'Mojito', size: 'M', unitPrice: 8.5, imageUrl: null }, 2);
  render(CartView, { global: { plugins: [router] } });
  return { router, cart };
}

describe('checkout flow', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    vi.mocked(api.createOrder).mockReset();
  });

  it('hides payment until a valid table number and enforces the 1..25 range', async () => {
    await setup();
    expect(screen.queryByText('Choisir le mode de paiement')).toBeNull();
    const table = screen.getByPlaceholderText('Ex. 12') as HTMLInputElement;

    // Placeholder and HTML max attribute reflect the 1..25 rule.
    expect(table.placeholder).toBe('Ex. 12');
    expect(table.getAttribute('max')).toBe('25');

    // Initial checkout warning is the exact required copy.
    expect(screen.getByText('Veuillez saisir votre numéro de table')).toBeTruthy();

    await fireEvent.update(table, '0');
    expect(screen.queryByText('Choisir le mode de paiement')).toBeNull();
    await fireEvent.update(table, '26');
    expect(screen.queryByText('Choisir le mode de paiement')).toBeNull();
    await fireEvent.update(table, '25');
    expect(screen.getByText('Choisir le mode de paiement')).toBeTruthy();
  });

  it('requires a payment method and sends the real enum + expanded quantities', async () => {
    vi.mocked(api.createOrder).mockResolvedValue(customerOrder());
    const { cart } = await setup();
    await fireEvent.update(screen.getByPlaceholderText('Ex. 12'), '12');

    const submit = screen.getByRole('button', { name: /Valider la commande/ }) as HTMLButtonElement;
    expect(submit.disabled).toBe(true); // payment not chosen yet

    await fireEvent.click(screen.getByDisplayValue('CARD_AT_COUNTER'));
    expect(submit.disabled).toBe(false);
    await fireEvent.click(submit);

    expect(api.createOrder).toHaveBeenCalledTimes(1);
    expect(api.createOrder).toHaveBeenCalledWith({
      items: [
        { cocktailId: 101, size: 'M' },
        { cocktailId: 101, size: 'M' },
      ],
      tableNumber: 12,
      paymentMethod: 'CARD_AT_COUNTER',
    });
    await waitFor(() => expect(cart.items).toHaveLength(0));
  });

  it('produces a single POST on a rapid double click', async () => {
    let resolve: (v: ReturnType<typeof customerOrder>) => void = () => {};
    vi.mocked(api.createOrder).mockReturnValue(new Promise((r) => { resolve = r; }));
    await setup();
    await fireEvent.update(screen.getByPlaceholderText('Ex. 12'), '12');
    await fireEvent.click(screen.getByDisplayValue('CARD_AT_COUNTER'));
    const submit = screen.getByRole('button', { name: /Valider la commande/ });
    await fireEvent.click(submit);
    await fireEvent.click(submit);
    expect(api.createOrder).toHaveBeenCalledTimes(1);
    resolve(customerOrder());
  });

  it('preserves the full cart, table and payment on a failed POST', async () => {
    vi.mocked(api.createOrder).mockRejectedValue(new ApiError({ message: 'boom', status: 500 }));
    const { cart, router } = await setup();
    const pushSpy = vi.spyOn(router, 'push');
    await fireEvent.update(screen.getByPlaceholderText('Ex. 12'), '12');
    await fireEvent.click(screen.getByDisplayValue('CARD_AT_COUNTER'));
    await fireEvent.click(screen.getByRole('button', { name: /Valider la commande/ }));

    await waitFor(() => expect(screen.getByRole('alert')).toBeTruthy());
    expect(cart.items).toHaveLength(1);
    expect(cart.itemCount).toBe(2);
    expect((screen.getByPlaceholderText('Ex. 12') as HTMLInputElement).value).toBe('12');
    expect(pushSpy).not.toHaveBeenCalledWith(expect.stringContaining('/client/confirmation/'));
  });
});
