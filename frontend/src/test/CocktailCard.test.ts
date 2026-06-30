import { fireEvent, render, screen } from '@testing-library/vue';
import { describe, expect, it } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import CocktailCard from '@/components/client/CocktailCard.vue';
import { mockCocktails } from '@/mocks/cocktails';
import { resolveCocktailImageSrc } from '@/utils/cocktailImages';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div />' } },
    { path: '/client/cocktails/:id', component: { template: '<div />' } },
  ],
});

describe('CocktailCard', () => {
  it('renders cocktail information and exposes detail/add actions', async () => {
    const result = render(CocktailCard, { props: { cocktail: mockCocktails[0], categoryName: 'Classiques' }, global: { plugins: [router] } });
    expect(await screen.findByText('Mojito')).toBeTruthy();
    expect(screen.getByText('Classiques')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Voir le détail de Mojito' })).toBeTruthy();
    const image = screen.getByAltText('Illustration du cocktail Mojito') as HTMLImageElement;
    expect(image.src).not.toContain('images.unsplash.com');
    expect(screen.getByText(/6,50/)).toBeTruthy();
    await fireEvent.click(screen.getByDisplayValue('L'));
    expect(screen.getByText(/10,50/)).toBeTruthy();
    await fireEvent.click(screen.getByRole('button', { name: 'Ajouter Mojito taille L au panier' }));
    expect(result.emitted().add?.[0]).toEqual([mockCocktails[0], 'L']);
  });

  it('preserves a supported cocktail image and replaces the temporary remote placeholder', () => {
    expect(resolveCocktailImageSrc('https://cdn.example.test/mojito.webp')).toBe('https://cdn.example.test/mojito.webp');
    expect(resolveCocktailImageSrc(mockCocktails[0].imageUrl)).not.toContain('images.unsplash.com');
  });
});
