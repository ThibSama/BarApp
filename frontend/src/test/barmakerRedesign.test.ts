import { fireEvent, render, screen, waitFor, within } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import CategoryManagementView from '@/views/barmaker/CategoryManagementView.vue';
import CocktailManagementView from '@/views/barmaker/CocktailManagementView.vue';
import { useCatalogStore } from '@/stores/catalog';

function router(path = '/bar/cocktails') {
  const instance = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/bar/categories', component: CategoryManagementView },
      { path: '/bar/cocktails', name: 'bar-cocktails', component: CocktailManagementView },
      { path: '/bar/cocktails/new', name: 'bar-cocktail-new', redirect: { name: 'bar-cocktails', query: { modal: 'create' } } },
      { path: '/bar/cocktails/:cocktailId/edit', name: 'bar-cocktail-edit', redirect: (to) => ({ name: 'bar-cocktails', query: { modal: 'edit', cocktailId: String(to.params.cocktailId) } }) },
    ],
  });
  return instance.push(path).then(() => instance);
}

async function fillValidCocktailForm(name = 'Cocktail test') {
  await fireEvent.update(screen.getByLabelText(/^Nom/), name);
  await fireEvent.update(screen.getByLabelText(/^Description courte/), 'Court');
  await fireEvent.update(screen.getByLabelText(/^Description complète/), 'Longue description');
  await fireEvent.update(screen.getByLabelText(/^URL de l’image/), 'https://example.com/test.png');
  await fireEvent.update(screen.getByLabelText('Ingrédient 1'), 'Citron');
  await fireEvent.update(screen.getByLabelText(/^Prix S/), '6');
  await fireEvent.update(screen.getByLabelText(/^Prix M/), '8');
  await fireEvent.update(screen.getByLabelText(/^Prix L/), '10');
}

describe('barmaker administration workflows (mock-backed)', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('keeps the category form hidden until the shared modal is opened, then cancels and restores focus', async () => {
    const r = await router('/bar/categories');
    render(CategoryManagementView, { global: { plugins: [r] } });
    const trigger = screen.getByRole('button', { name: 'Créer une catégorie' });
    expect(screen.queryByRole('dialog', { name: 'Créer une catégorie' })).toBeNull();

    trigger.focus();
    await fireEvent.click(trigger);
    const dialog = screen.getByRole('dialog', { name: 'Créer une catégorie' });
    expect(dialog.parentElement?.className).toContain('modal-backdrop');
    expect(dialog.className).toContain('modal-panel--compact');
    expect(dialog.querySelector('.modal-handle')).toBeTruthy();
    expect(document.body.style.overflow).toBe('hidden');
    expect(dialog.querySelector('.modal-body')).toBeTruthy();
    expect(within(dialog).getByLabelText(/Nom/)).toBe(document.activeElement);

    await fireEvent.click(within(dialog).getByRole('button', { name: 'Annuler' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Créer une catégorie' })).toBeNull());
    expect(document.activeElement).toBe(trigger);
  });

  it('validates, creates, edits, toggles and confirms deletion of a category in modals', async () => {
    const r = await router('/bar/categories');
    const catalog = useCatalogStore();
    render(CategoryManagementView, { global: { plugins: [r] } });
    await fireEvent.click(screen.getByRole('button', { name: 'Créer une catégorie' }));
    await fireEvent.click(screen.getByRole('button', { name: 'Créer la catégorie' }));
    expect(screen.getByText('Nom est obligatoire.')).toBeTruthy();

    await fireEvent.update(screen.getByLabelText(/Nom/), 'Tests visuels');
    await fireEvent.update(screen.getByLabelText(/Description/), 'Catégorie temporaire');
    await fireEvent.click(screen.getByRole('button', { name: 'Créer la catégorie' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Créer une catégorie' })).toBeNull());
    const created = catalog.categories.find((category) => category.name === 'Tests visuels')!;
    expect(created).toBeTruthy();

    const card = screen.getByText('Tests visuels').closest('article')!;
    await fireEvent.click(within(card as HTMLElement).getByRole('button', { name: 'Modifier' }));
    await fireEvent.update(screen.getByLabelText(/Nom/), 'Tests visuels modifiés');
    await fireEvent.click(screen.getByRole('button', { name: 'Enregistrer les modifications' }));
    expect(catalog.getCategoryById(created.id)?.name).toBe('Tests visuels modifiés');
    await fireEvent.click(within(card as HTMLElement).getByRole('button', { name: 'Désactiver' }));
    expect(catalog.getCategoryById(created.id)?.enabled).toBe(false);
    await fireEvent.click(screen.getByRole('button', { name: /Supprimer Tests visuels/ }));
    expect(screen.getByRole('dialog')).toBeTruthy();
    await fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Annuler' }));
    expect(catalog.getCategoryById(created.id)).toBeTruthy();
  });

  it('keeps the category modal open when persistence fails', async () => {
    const r = await router('/bar/categories');
    const catalog = useCatalogStore();
    const createSpy = vi.spyOn(catalog, 'createCategory').mockImplementation(() => { throw new Error('fail'); });
    render(CategoryManagementView, { global: { plugins: [r] } });
    await fireEvent.click(screen.getByRole('button', { name: 'Créer une catégorie' }));
    await fireEvent.update(screen.getByLabelText(/Nom/), 'Erreur catégorie');
    await fireEvent.update(screen.getByLabelText(/Description/), 'Doit rester ouvert');
    await fireEvent.click(screen.getByRole('button', { name: 'Créer la catégorie' }));
    expect(screen.getByRole('dialog', { name: 'Créer une catégorie' })).toBeTruthy();
    expect(screen.getByRole('alert').textContent).toContain('Impossible d’enregistrer cette catégorie');
    createSpy.mockRestore();
  });

  it('opens cocktail creation in a modal from the list, manages fields, validates, cancels and creates with the existing payload', async () => {
    const r = await router('/bar/cocktails');
    const catalog = useCatalogStore();
    const createSpy = vi.spyOn(catalog, 'createCocktail');
    render(CocktailManagementView, { global: { plugins: [r] } });

    expect(screen.queryByRole('dialog', { name: 'Créer un cocktail' })).toBeNull();
    expect(screen.getByText('Mojito')).toBeTruthy();
    await fireEvent.click(screen.getByRole('button', { name: 'Créer un cocktail' }));
    const dialog = await screen.findByRole('dialog', { name: 'Créer un cocktail' });
    expect(dialog.parentElement?.className).toContain('modal-backdrop');
    expect(dialog.className).toContain('modal-panel--large');
    expect(dialog.querySelector('.modal-handle')).toBeTruthy();
    expect(document.body.style.overflow).toBe('hidden');
    const modalBody = dialog.querySelector('.modal-body') as HTMLElement | null;
    expect(modalBody).toBeTruthy();
    expect(modalBody?.style.overflowY).not.toBe('visible');
    expect(within(dialog).getByRole('heading', { name: 'Informations générales' })).toBeTruthy();
    expect(within(dialog).getByRole('heading', { name: 'Image' })).toBeTruthy();
    expect(within(dialog).getByRole('heading', { name: 'Ingrédients' })).toBeTruthy();
    expect(within(dialog).getByRole('heading', { name: 'Tailles et prix' })).toBeTruthy();
    expect(within(dialog).getByRole('heading', { name: 'Disponibilité' })).toBeTruthy();

    await fireEvent.click(within(dialog).getByRole('button', { name: 'Enregistrer' }));
    expect(within(dialog).getByText('Nom est obligatoire.')).toBeTruthy();
    await fillValidCocktailForm();
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Ajouter un ingrédient' }));
    expect(within(dialog).getByLabelText('Ingrédient 2')).toBeTruthy();
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Retirer l’ingrédient 2' }));
    expect(within(dialog).queryByLabelText('Ingrédient 2')).toBeNull();
    await fireEvent.click(within(dialog).getByLabelText('Cocktail disponible'));
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Annuler' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Créer un cocktail' })).toBeNull());

    await fireEvent.click(screen.getByRole('button', { name: 'Créer un cocktail' }));
    await fillValidCocktailForm('Cocktail payload');
    await fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Créer un cocktail' })).toBeNull());
    expect(createSpy).toHaveBeenCalledWith(expect.objectContaining({ name: 'Cocktail payload', ingredients: ['Citron'], prices: { S: 6, M: 8, L: 10 } }));
    expect(catalog.cocktails.some((cocktail) => cocktail.name === 'Cocktail payload')).toBe(true);
  });

  it('keeps the cocktail modal open when persistence fails', async () => {
    const r = await router('/bar/cocktails');
    const catalog = useCatalogStore();
    const createSpy = vi.spyOn(catalog, 'createCocktail').mockImplementation(() => { throw new Error('fail'); });
    render(CocktailManagementView, { global: { plugins: [r] } });
    await fireEvent.click(screen.getByRole('button', { name: 'Créer un cocktail' }));
    await fillValidCocktailForm('Cocktail erreur');
    await fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(screen.getByRole('dialog', { name: 'Créer un cocktail' })).toBeTruthy();
    expect(screen.getByRole('alert').textContent).toContain('Impossible d’enregistrer ce cocktail');
    createSpy.mockRestore();
  });

  it('opens cocktail editing in the same modal from the list and updates the existing cocktail', async () => {
    const r = await router('/bar/cocktails');
    const catalog = useCatalogStore();
    const updateSpy = vi.spyOn(catalog, 'updateCocktail');
    render(CocktailManagementView, { global: { plugins: [r] } });
    const mojitoRow = screen.getByRole('heading', { name: 'Mojito' }).closest('article') as HTMLElement;

    await fireEvent.click(within(mojitoRow).getByRole('button', { name: 'Modifier' }));
    const dialog = screen.getByRole('dialog', { name: 'Modifier le cocktail' });
    expect(dialog.parentElement?.className).toContain('modal-backdrop');
    expect(dialog.className).toContain('modal-panel--large');
    expect(screen.getByText('Mojito')).toBeTruthy();
    expect(r.currentRoute.value.name).toBe('bar-cocktails');
    expect(r.currentRoute.value.query).toMatchObject({ modal: 'edit', cocktailId: 'mojito' });
    expect(within(dialog).getByDisplayValue('Mojito')).toBeTruthy();

    await fireEvent.update(within(dialog).getByLabelText(/^Nom/), 'Mojito édition');
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Modifier le cocktail' })).toBeNull());

    expect(updateSpy).toHaveBeenCalledWith('mojito', expect.objectContaining({ name: 'Mojito édition' }));
    expect(catalog.getCocktailById('mojito')?.name).toBe('Mojito édition');
    expect(r.currentRoute.value.query.modal).toBeUndefined();
    updateSpy.mockRestore();
  });

  it('handles the old cocktail creation route by opening the modal on the list', async () => {
    const r = await router('/bar/cocktails/new');
    expect(r.currentRoute.value.name).toBe('bar-cocktails');
    expect(r.currentRoute.value.query.modal).toBe('create');
    render(CocktailManagementView, { global: { plugins: [r] } });
    expect(screen.getByText('Mojito')).toBeTruthy();
    expect(screen.getByRole('dialog', { name: 'Créer un cocktail' })).toBeTruthy();
    await fireEvent.click(screen.getByRole('button', { name: 'Fermer le formulaire cocktail' }));
    await waitFor(() => expect(r.currentRoute.value.query.modal).toBeUndefined());
  });

  it('redirects the old cocktail edit route to the list edit modal', async () => {
    const r = await router('/bar/cocktails/mojito/edit');
    expect(r.currentRoute.value.name).toBe('bar-cocktails');
    expect(r.currentRoute.value.query).toMatchObject({ modal: 'edit', cocktailId: 'mojito' });
    render(CocktailManagementView, { global: { plugins: [r] } });
    expect(screen.getByText('Mojito')).toBeTruthy();
    expect(screen.getByRole('dialog', { name: 'Modifier le cocktail' })).toBeTruthy();
  });
});
