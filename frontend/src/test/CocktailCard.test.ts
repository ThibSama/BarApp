import { fireEvent, render, screen } from '@testing-library/vue';
import { describe, expect, it } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import CocktailCard from '@/components/client/CocktailCard.vue';
import type { MenuCocktailView } from '@/stores/menu';
import { menuResponse } from './fixtures/catalog';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div />' } },
    { path: '/client/cocktails/:id', component: { template: '<div />' } },
  ],
});

function mojitoView(): MenuCocktailView {
  const category = menuResponse().categories[0];
  return { ...category.cocktails[0], categoryId: category.id, categoryName: category.name };
}

describe('CocktailCard', () => {
  it('renders real menu data and selects a size with its real price', async () => {
    const cocktail = mojitoView();
    const result = render(CocktailCard, { props: { cocktail, categoryName: 'Classiques' }, global: { plugins: [router] } });
    expect(await screen.findByText('Mojito')).toBeTruthy();
    expect(screen.getByText('Classiques')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Voir le détail de Mojito' })).toBeTruthy();

    // Default size S → 6,50 €; switching to L shows 10,50 €.
    expect(screen.getByText(/6,50/)).toBeTruthy();
    await fireEvent.click(screen.getByDisplayValue('L'));
    expect(screen.getByText(/10,50/)).toBeTruthy();

    await fireEvent.click(screen.getByRole('button', { name: 'Ajouter Mojito taille L au panier' }));
    expect(result.emitted().add?.[0]).toEqual([cocktail, 'L']);
  });

  it('only shows the sizes the backend returns and falls back to a safe image', () => {
    const cocktail = mojitoView();
    cocktail.prices = [{ size: 'S', price: 6.5 }, { size: 'M', price: 8.5 }]; // no L
    render(CocktailCard, { props: { cocktail, categoryName: 'Classiques' }, global: { plugins: [router] } });
    expect(screen.getByDisplayValue('S')).toBeTruthy();
    expect(screen.getByDisplayValue('M')).toBeTruthy();
    expect(screen.queryByDisplayValue('L')).toBeNull();
    const image = screen.getByAltText('Illustration du cocktail Mojito') as HTMLImageElement;
    expect(image.src).not.toContain('images.unsplash.com');
  });
});
