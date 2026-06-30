import { fireEvent, render, screen, within } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import MenuView from '@/views/client/MenuView.vue';
import CocktailDetailsView from '@/views/client/CocktailDetailsView.vue';
import CartView from '@/views/client/CartView.vue';
import { useCartStore } from '@/stores/cart';

function createTestRouter(path = '/client/menu') {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/client/menu', component: MenuView },
      { path: '/client/cocktails/:id', component: CocktailDetailsView },
      { path: '/client/panier', component: CartView },
      { path: '/client/confirmation/:orderId', component: { template: '<div>Confirmation</div>' } },
    ],
  });
  return router.push(path).then(() => router);
}

describe('customer redesign behavior', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('filters the menu by cocktail name, ingredient and category, then shows empty state', async () => {
    const router = await createTestRouter();
    render(MenuView, { global: { plugins: [router] } });

    const search = screen.getByRole('searchbox');
    await fireEvent.update(search, 'Mojito');
    expect(screen.getByText('Mojito')).toBeTruthy();
    expect(screen.queryByText('Piña Colada')).toBeNull();

    await fireEvent.click(screen.getByRole('button', { name: 'Effacer la recherche' }));
    await fireEvent.update(search, 'concombre');
    expect(screen.getByText('Jardin Vert')).toBeTruthy();

    await fireEvent.click(screen.getByRole('button', { name: 'Fruité' }));
    expect(screen.getByText('Aucun cocktail trouvé')).toBeTruthy();

    await fireEvent.update(search, 'zzzz');
    expect(screen.getByText('Essayez un autre ingrédient ou une autre catégorie.')).toBeTruthy();
  });

  it('selects a cocktail size directly on a menu card before adding it to the basket', async () => {
    const router = await createTestRouter();
    render(MenuView, { global: { plugins: [router] } });
    const cart = useCartStore();
    const mojitoCard = screen.getByRole('heading', { name: 'Mojito' }).closest('article') as HTMLElement;

    expect(within(mojitoCard).getByText(/6,50/)).toBeTruthy();
    await fireEvent.click(within(mojitoCard).getByDisplayValue('L'));
    expect(within(mojitoCard).getByText(/10,50/)).toBeTruthy();
    await fireEvent.click(within(mojitoCard).getByRole('button', { name: 'Ajouter Mojito taille L au panier' }));

    expect(cart.items).toHaveLength(1);
    expect(cart.items[0]).toMatchObject({ cocktailId: 'mojito', size: 'L', quantity: 1 });
    expect(screen.getAllByText('Mojito taille L a été ajouté au panier.').length).toBeGreaterThan(0);

    await fireEvent.click(within(mojitoCard).getByDisplayValue('M'));
    await fireEvent.click(within(mojitoCard).getByRole('button', { name: 'Ajouter Mojito taille M au panier' }));
    expect(cart.items).toHaveLength(2);
    expect(cart.items.some((item) => item.cocktailId === 'mojito' && item.size === 'M')).toBe(true);
  });

  it('opens cocktail details, updates size/quantity price and adds to basket without going below one', async () => {
    const router = await createTestRouter('/client/cocktails/mojito');
    render(CocktailDetailsView, { global: { plugins: [router] } });
    expect(screen.getByRole('heading', { name: 'Mojito' })).toBeTruthy();

    await fireEvent.click(screen.getByDisplayValue('L'));
    expect(screen.getAllByText(/10,50/).length).toBeGreaterThan(0);

    const decrease = screen.getByRole('button', { name: /diminuer la quantité/i }) as HTMLButtonElement;
    expect(decrease.disabled).toBe(true);
    await fireEvent.click(screen.getByRole('button', { name: /augmenter la quantité/i }));
    expect(screen.getByText(/21,00/)).toBeTruthy();

    await fireEvent.click(screen.getByRole('button', { name: 'Ajouter au panier' }));
    expect(useCartStore().itemCount).toBe(2);
    expect(screen.getAllByText('Le cocktail a été ajouté au panier.').length).toBeGreaterThan(0);
  });

  it('updates basket quantities, totals, removal and empty basket state', async () => {
    const router = await createTestRouter('/client/panier');
    const cart = useCartStore();
    cart.addItem('mojito', 'M', 1);
    render(CartView, { global: { plugins: [router] } });

    expect(screen.getByRole('heading', { name: 'Votre panier' })).toBeTruthy();
    expect(screen.getAllByText(/8,50/).length).toBeGreaterThan(0);

    await fireEvent.click(screen.getByRole('button', { name: /augmenter la quantité/i }));
    expect(cart.itemCount).toBe(2);
    expect(screen.getAllByText(/17,00/).length).toBeGreaterThan(0);

    await fireEvent.click(screen.getByRole('button', { name: /Retirer Mojito/ }));
    expect(screen.getByText('Votre panier est vide')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Parcourir la carte' })).toBeTruthy();
  });
});
