import { render, screen } from '@testing-library/vue';
import { createPinia } from 'pinia';
import { describe, expect, it } from 'vitest';
import App from '@/App.vue';
import router from '@/router';

describe('routing', () => {
  it('redirects the root route to the cocktail menu', async () => {
    await router.push('/');
    await router.isReady();
    expect(router.currentRoute.value.path).toBe('/client/menu');
  });

  it('does not show the old landing page in the normal route flow', async () => {
    await router.push('/');
    render(App, { global: { plugins: [createPinia(), router] } });
    expect(screen.queryByText("Bienvenue sur Le Bar'app")).toBeNull();
    expect(screen.queryByText('Prototype de démonstration')).toBeNull();
    expect(await screen.findByRole('heading', { name: 'Carte des cocktails' })).toBeTruthy();
  });
});
