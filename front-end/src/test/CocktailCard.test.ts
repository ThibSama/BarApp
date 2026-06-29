import { render, screen } from '@testing-library/vue';
import { describe, expect, it } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import CocktailCard from '@/components/client/CocktailCard.vue';
import { mockCocktails } from '@/mocks/cocktails';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div />' } },
    { path: '/client/cocktails/:id', component: { template: '<div />' } },
  ],
});

describe('CocktailCard', () => {
  it('renders cocktail information in French', async () => {
    render(CocktailCard, { props: { cocktail: mockCocktails[0], categoryName: 'Classiques' }, global: { plugins: [router] } });
    expect(await screen.findByText('Mojito')).toBeTruthy();
    expect(screen.getByText('Disponible')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Voir le détail' })).toBeTruthy();
  });
});
