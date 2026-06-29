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
    ],
  });
}

describe('client header', () => {
  it('exposes only the public client links', async () => {
    const router = createTestRouter();
    await router.push('/client/menu');
    render(ClientLayout, { global: { plugins: [createPinia(), router] } });
    const navigation = screen.getByRole('navigation', { name: 'Navigation client' });
    expect(within(navigation).getByRole('link', { name: 'Carte' })).toBeTruthy();
    expect(within(navigation).getByRole('link', { name: /Panier/ })).toBeTruthy();
    expect(within(navigation).queryByText('Espace barmaker')).toBeNull();
    expect(within(navigation).queryByText('Espace client')).toBeNull();
  });
});
