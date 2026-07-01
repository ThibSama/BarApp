import { fireEvent, render, screen, waitFor, within } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import CategoryManagementView from '@/views/barmaker/CategoryManagementView.vue';
import CocktailManagementView from '@/views/barmaker/CocktailManagementView.vue';
import { ApiError } from '@/services/apiClient';
import { categoryResponses, cocktailResponse, cocktailResponses, ingredientResponses } from './fixtures/catalog';

vi.mock('@/services/catalogAdminApi', () => ({
  fetchCategories: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
  deleteCategory: vi.fn(),
  fetchCocktails: vi.fn(),
  fetchCocktail: vi.fn(),
  createCocktail: vi.fn(),
  updateCocktail: vi.fn(),
  deleteCocktail: vi.fn(),
  fetchIngredients: vi.fn(),
}));
import * as adminApi from '@/services/catalogAdminApi';

function router(path = '/bar/cocktails') {
  const instance = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/bar/categories', component: CategoryManagementView },
      { path: '/bar/cocktails', name: 'bar-cocktails', component: CocktailManagementView },
      { path: '/bar/cocktails/new', name: 'bar-cocktail-new', redirect: { name: 'bar-cocktails', query: { modal: 'create' } } },
      { path: '/bar/cocktails/:cocktailId/edit', name: 'bar-cocktail-edit', redirect: (to) => `/bar/cocktails?modal=edit&cocktailId=${encodeURIComponent(String(to.params.cocktailId))}` },
    ],
  });
  return instance.push(path).then(() => instance);
}

async function fillValidCocktailForm(name = 'Cocktail test') {
  await fireEvent.update(screen.getByLabelText(/^Nom/), name);
  await fireEvent.update(screen.getByLabelText(/^Description/), 'Longue description');
  await fireEvent.update(screen.getByLabelText('Ingrédient 1'), 'Citron');
  await fireEvent.update(screen.getByLabelText(/^Prix S/), '6');
  await fireEvent.update(screen.getByLabelText(/^Prix M/), '8');
  await fireEvent.update(screen.getByLabelText(/^Prix L/), '10');
}

describe('barmaker catalogue administration (real admin API)', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    document.body.style.overflow = '';
    setActivePinia(createPinia());
    vi.mocked(adminApi.fetchCategories).mockReset().mockResolvedValue(categoryResponses());
    vi.mocked(adminApi.createCategory).mockReset();
    vi.mocked(adminApi.updateCategory).mockReset();
    vi.mocked(adminApi.deleteCategory).mockReset().mockResolvedValue(undefined);
    vi.mocked(adminApi.fetchCocktails).mockReset().mockResolvedValue(cocktailResponses());
    vi.mocked(adminApi.fetchCocktail).mockReset().mockResolvedValue(cocktailResponse());
    vi.mocked(adminApi.createCocktail).mockReset();
    vi.mocked(adminApi.updateCocktail).mockReset();
    vi.mocked(adminApi.deleteCocktail).mockReset().mockResolvedValue(undefined);
    vi.mocked(adminApi.fetchIngredients).mockReset().mockResolvedValue(ingredientResponses());
  });

  it('lists active and inactive categories from the API', async () => {
    const r = await router('/bar/categories');
    render(CategoryManagementView, { global: { plugins: [r] } });
    expect(await screen.findByText('Classiques')).toBeTruthy();
    expect(screen.getByText('Sans alcool')).toBeTruthy();
    expect(screen.getByText('Désactivée')).toBeTruthy();
  });

  it('creates a category through the modal and shows a success toast', async () => {
    vi.mocked(adminApi.createCategory).mockResolvedValue({ id: 9, name: 'Tests', description: null, displayOrder: 4, active: true });
    const r = await router('/bar/categories');
    render(CategoryManagementView, { global: { plugins: [r] } });
    await screen.findByText('Classiques');

    await fireEvent.click(screen.getByRole('button', { name: 'Créer une catégorie' }));
    await fireEvent.click(screen.getByRole('button', { name: 'Créer la catégorie' }));
    expect(screen.getByText('Nom est obligatoire.')).toBeTruthy();

    await fireEvent.update(screen.getByLabelText(/Nom/), 'Tests');
    await fireEvent.click(screen.getByRole('button', { name: 'Créer la catégorie' }));
    await waitFor(() => expect(adminApi.createCategory).toHaveBeenCalled());
    expect(adminApi.createCategory).toHaveBeenCalledWith(expect.objectContaining({ name: 'Tests', active: true }));
    expect(await screen.findByText('Catégorie créée')).toBeTruthy();
  });

  it('keeps the modal open and flags the name on a 409 conflict', async () => {
    vi.mocked(adminApi.createCategory).mockRejectedValue(new ApiError({ message: 'x', status: 409, code: 'CATEGORY_ALREADY_EXISTS' }));
    const r = await router('/bar/categories');
    render(CategoryManagementView, { global: { plugins: [r] } });
    await screen.findByText('Classiques');
    await fireEvent.click(screen.getByRole('button', { name: 'Créer une catégorie' }));
    await fireEvent.update(screen.getByLabelText(/Nom/), 'Classiques');
    await fireEvent.click(screen.getByRole('button', { name: 'Créer la catégorie' }));
    await waitFor(() => expect(screen.getByText('Une catégorie portant ce nom existe déjà.')).toBeTruthy());
    expect(screen.getByRole('dialog', { name: 'Créer une catégorie' })).toBeTruthy();
  });

  it('deactivates a category via DELETE after confirmation', async () => {
    vi.mocked(adminApi.fetchCategories)
      .mockResolvedValueOnce(categoryResponses())
      .mockResolvedValue(categoryResponses().map((c) => (c.id === 1 ? { ...c, active: false } : c)));
    const r = await router('/bar/categories');
    render(CategoryManagementView, { global: { plugins: [r] } });
    const card = (await screen.findByText('Classiques')).closest('article') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Désactiver/ }));
    await fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Désactiver' }));
    await waitFor(() => expect(adminApi.deleteCategory).toHaveBeenCalledWith(1));
    expect(await screen.findByText('Catégorie désactivée')).toBeTruthy();
  });

  it('reactivates an inactive category via PUT active=true', async () => {
    vi.mocked(adminApi.updateCategory).mockResolvedValue({ ...categoryResponses()[2], active: true });
    const r = await router('/bar/categories');
    render(CategoryManagementView, { global: { plugins: [r] } });
    const card = (await screen.findByText('Sans alcool')).closest('article') as HTMLElement;
    await fireEvent.click(within(card).getByRole('button', { name: /Activer/ }));
    await waitFor(() => expect(adminApi.updateCategory).toHaveBeenCalledWith(3, expect.objectContaining({ active: true })));
    expect(await screen.findByText('Catégorie réactivée')).toBeTruthy();
  });

  it('lists cocktails and opens the creation modal, validating then sending the exact payload', async () => {
    vi.mocked(adminApi.createCocktail).mockResolvedValue(cocktailResponse({ id: 200, name: 'Cocktail test' }));
    const r = await router('/bar/cocktails');
    render(CocktailManagementView, { global: { plugins: [r] } });
    expect(await screen.findByText('Mojito')).toBeTruthy();

    await fireEvent.click(screen.getByRole('button', { name: 'Créer un cocktail' }));
    const dialog = await screen.findByRole('dialog', { name: 'Créer un cocktail' });
    expect(dialog.className).toContain('modal-panel--large');

    // Exactly one description field, labelled "Description" (no short/long split).
    expect(within(dialog).getAllByLabelText(/^Description/)).toHaveLength(1);
    expect(within(dialog).getByLabelText('Description').tagName).toBe('TEXTAREA');
    expect(within(dialog).queryByLabelText('Description courte')).toBeNull();
    expect(within(dialog).queryByLabelText('Description complète')).toBeNull();

    await fireEvent.click(within(dialog).getByRole('button', { name: 'Enregistrer' }));
    expect(within(dialog).getByText('Le nom est obligatoire.')).toBeTruthy();

    await fillValidCocktailForm('Cocktail payload');
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(adminApi.createCocktail).toHaveBeenCalled());
    expect(adminApi.createCocktail).toHaveBeenCalledWith(
      expect.objectContaining({
        categoryId: 1,
        name: 'Cocktail payload',
        description: 'Longue description',
        ingredients: [{ name: 'Citron', quantityLabel: null, displayOrder: 0 }],
        prices: [
          { size: 'S', price: 6 },
          { size: 'M', price: 8 },
          { size: 'L', price: 10 },
        ],
      }),
    );
    // The obsolete field is no longer part of the payload.
    expect(vi.mocked(adminApi.createCocktail).mock.calls[0][0]).not.toHaveProperty('shortDescription');
    expect(await screen.findByText('Cocktail créé')).toBeTruthy();
  });

  it('rejects duplicate ingredient names in the cocktail form', async () => {
    const r = await router('/bar/cocktails');
    render(CocktailManagementView, { global: { plugins: [r] } });
    await screen.findByText('Mojito');
    await fireEvent.click(screen.getByRole('button', { name: 'Créer un cocktail' }));
    const dialog = await screen.findByRole('dialog', { name: 'Créer un cocktail' });
    await fillValidCocktailForm('Doublons');
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Ajouter un ingrédient' }));
    await fireEvent.update(within(dialog).getByLabelText('Ingrédient 2'), 'citron');
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Enregistrer' }));
    expect(within(dialog).getByText(/doit être unique/)).toBeTruthy();
    expect(adminApi.createCocktail).not.toHaveBeenCalled();
  });

  it('opens edit in the same modal, loading the detail and updating', async () => {
    vi.mocked(adminApi.updateCocktail).mockResolvedValue(cocktailResponse({ name: 'Mojito édition' }));
    const r = await router('/bar/cocktails');
    render(CocktailManagementView, { global: { plugins: [r] } });
    const row = (await screen.findByRole('heading', { name: 'Mojito' })).closest('article') as HTMLElement;
    await fireEvent.click(within(row).getByRole('button', { name: 'Modifier' }));
    const dialog = await screen.findByRole('dialog', { name: 'Modifier le cocktail' });
    await waitFor(() => expect(adminApi.fetchCocktail).toHaveBeenCalledWith(101));
    await waitFor(() => expect(within(dialog).getByDisplayValue('Mojito')).toBeTruthy());

    await fireEvent.update(within(dialog).getByLabelText(/^Nom/), 'Mojito édition');
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(adminApi.updateCocktail).toHaveBeenCalledWith(101, expect.objectContaining({ name: 'Mojito édition' })));
    expect(await screen.findByText('Cocktail modifié')).toBeTruthy();
  });

  it('redirects the old cocktail edit route to the list edit modal', async () => {
    const r = await router('/bar/cocktails/101/edit');
    expect(r.currentRoute.value.name).toBe('bar-cocktails');
    expect(r.currentRoute.value.query).toMatchObject({ modal: 'edit', cocktailId: '101' });
    render(CocktailManagementView, { global: { plugins: [r] } });
    expect(await screen.findByRole('dialog', { name: 'Modifier le cocktail' })).toBeTruthy();
  });
});
