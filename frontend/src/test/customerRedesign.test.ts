import { fireEvent, render, screen, within } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import MenuView from '@/views/client/MenuView.vue';
import CocktailDetailsView from '@/views/client/CocktailDetailsView.vue';
import CartView from '@/views/client/CartView.vue';
import { useCartStore } from '@/stores/cart';
import { menuResponse } from './fixtures/catalog';

vi.mock('@/services/menuApi', () => ({ fetchMenu: vi.fn() }));
import * as menuApi from '@/services/menuApi';

function createTestRouter(path = '/client/menu') {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/client/menu', name: 'client-menu', component: MenuView },
      { path: '/client/cocktails/:id', name: 'client-cocktail-details', component: CocktailDetailsView },
      { path: '/client/panier', name: 'client-cart', component: CartView },
      { path: '/client/confirmation/:orderId', name: 'client-order-confirmation', component: { template: '<div>Confirmation</div>' } },
    ],
  });
  return router.push(path).then(() => router);
}

function getToast(): HTMLElement {
  const toast = document.body.querySelector('.success-toast') as HTMLElement | null;
  if (!toast) throw new Error('Success toast not found');
  return toast;
}

describe('customer redesign behavior (real menu API)', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    vi.mocked(menuApi.fetchMenu).mockReset().mockResolvedValue(menuResponse());
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('loads the real menu and filters by name, ingredient and category', async () => {
    const router = await createTestRouter();
    render(MenuView, { global: { plugins: [router] } });

    expect(await screen.findByText('Mojito')).toBeTruthy();
    const search = screen.getByRole('searchbox');
    await fireEvent.update(search, 'Mojito');
    expect(screen.getByText('Mojito')).toBeTruthy();
    expect(screen.queryByText('Piña Colada')).toBeNull();

    await fireEvent.click(screen.getByRole('button', { name: 'Effacer la recherche' }));
    await fireEvent.update(search, 'concombre');
    expect(screen.getByText('Jardin Vert')).toBeTruthy();

    await fireEvent.click(screen.getByRole('button', { name: 'Fruité' }));
    expect(screen.getByText('Aucun cocktail trouvé')).toBeTruthy();
  });

  it('shows an error + retry when the menu API fails, never fake data', async () => {
    vi.mocked(menuApi.fetchMenu).mockRejectedValueOnce(new Error('down'));
    const router = await createTestRouter();
    render(MenuView, { global: { plugins: [router] } });
    expect(await screen.findByText('Carte indisponible')).toBeTruthy();

    vi.mocked(menuApi.fetchMenu).mockResolvedValueOnce(menuResponse());
    await fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }));
    expect(await screen.findByText('Mojito')).toBeTruthy();
  });

  it('selects a size directly on a card and adds separate lines per size', async () => {
    const router = await createTestRouter();
    render(MenuView, { global: { plugins: [router] } });
    const cart = useCartStore();
    const mojitoCard = (await screen.findByRole('heading', { name: 'Mojito' })).closest('article') as HTMLElement;

    expect(within(mojitoCard).getByText(/6,50/)).toBeTruthy();
    await fireEvent.click(within(mojitoCard).getByDisplayValue('L'));
    expect(within(mojitoCard).getByText(/10,50/)).toBeTruthy();
    await fireEvent.click(within(mojitoCard).getByRole('button', { name: 'Ajouter Mojito taille L au panier' }));

    expect(cart.items).toHaveLength(1);
    expect(cart.items[0]).toMatchObject({ cocktailId: 101, size: 'L', quantity: 1 });
    expect(getToast().textContent).toContain('Mojito taille L a été ajouté au panier.');

    await fireEvent.click(within(mojitoCard).getByDisplayValue('M'));
    await fireEvent.click(within(mojitoCard).getByRole('button', { name: 'Ajouter Mojito taille M au panier' }));
    expect(cart.items).toHaveLength(2);
  });

  it('opens the detail view via direct load and updates size/quantity price', async () => {
    vi.useFakeTimers();
    const router = await createTestRouter('/client/cocktails/101');
    render(CocktailDetailsView, { global: { plugins: [router] } });
    await vi.waitFor(() => expect(screen.getByRole('heading', { name: 'Mojito' })).toBeTruthy());

    await fireEvent.click(screen.getByDisplayValue('L'));
    expect(screen.getAllByText(/10,50/).length).toBeGreaterThan(0);

    const decrease = screen.getByRole('button', { name: /diminuer la quantité/i }) as HTMLButtonElement;
    expect(decrease.disabled).toBe(true);
    await fireEvent.click(screen.getByRole('button', { name: /augmenter la quantité/i }));
    expect(screen.getByText(/21,00/)).toBeTruthy();

    await fireEvent.click(screen.getByRole('button', { name: 'Ajouter au panier' }));
    expect(useCartStore().itemCount).toBe(2);
  });

  it('updates basket quantities, totals and removal', async () => {
    const router = await createTestRouter('/client/panier');
    const cart = useCartStore();
    cart.addItem({ cocktailId: 101, name: 'Mojito', size: 'M', unitPrice: 8.5, imageUrl: null }, 1);
    render(CartView, { global: { plugins: [router] } });

    expect(screen.getByRole('heading', { name: 'Votre panier' })).toBeTruthy();
    expect(screen.getAllByText(/8,50/).length).toBeGreaterThan(0);

    await fireEvent.click(screen.getByRole('button', { name: /augmenter la quantité/i }));
    expect(cart.itemCount).toBe(2);
    expect(screen.getAllByText(/17,00/).length).toBeGreaterThan(0);

    await fireEvent.click(screen.getByRole('button', { name: /Retirer Mojito/ }));
    expect(screen.getByText('Votre panier est vide')).toBeTruthy();
  });
});
