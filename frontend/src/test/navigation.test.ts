import { render, screen, within } from '@testing-library/vue';
import { createPinia } from 'pinia';
import { describe, expect, it } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import ClientLayout from '@/layouts/ClientLayout.vue';

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/client/menu', component: { template: '<div>Carte</div>' } },
      { path: '/client/panier', component: { template: '<div>Panier</div>' } },
      { path: '/client/suivi', component: { template: '<div>Suivi</div>' } },
    ],
  });
}

describe('client header', () => {
  it('exposes only the public client links', async () => {
    const router = createTestRouter();
    await router.push('/client/menu');
    render(ClientLayout, { global: { plugins: [createPinia(), router] } });
    const navigation = screen.getByRole('navigation', { name: 'Navigation client' });
    expect(within(navigation).getByRole('link', { name: 'Carte des cocktails' })).toBeTruthy();
    expect(within(navigation).getByRole('link', { name: /Panier/ })).toBeTruthy();
    expect(within(navigation).getByRole('link', { name: 'Ma commande' })).toBeTruthy();
    expect(within(navigation).queryByText('Espace barmaker')).toBeNull();
    expect(within(navigation).queryByText('Espace client')).toBeNull();
    expect(within(navigation).queryByText(/compte|profil|connexion|inscription/i)).toBeNull();
  });

  it('shows the approved brand and subtitle in the customer sidebar', async () => {
    const router = createTestRouter();
    await router.push('/client/menu');
    render(ClientLayout, { global: { plugins: [createPinia(), router] } });
    const sidebar = screen.getByRole('complementary', { name: 'Navigation client bureau' });
    expect(within(sidebar).getByText(/LE BAR.APP/)).toBeTruthy();
    expect(within(sidebar).getByText('COCKTAILS & GOOD VIBES')).toBeTruthy();
    expect(within(sidebar).queryByText(/espace barmaker/i)).toBeNull();
  });
});
