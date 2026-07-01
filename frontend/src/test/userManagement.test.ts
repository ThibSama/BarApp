import { fireEvent, render, screen, waitFor, within } from '@testing-library/vue';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import UserManagementView from '@/views/barmaker/UserManagementView.vue';
import { ApiError } from '@/services/apiClient';
import type { UserAdminResponse } from '@/types/api';

vi.mock('@/services/usersApi', () => ({
  fetchUsers: vi.fn(),
  createBarmaker: vi.fn(),
}));
import * as usersApi from '@/services/usersApi';

const staff: UserAdminResponse[] = [
  {
    id: 1,
    username: 'barmaker',
    displayName: 'Barman principal',
    role: 'BARMAKER',
    active: true,
    createdAt: '2026-01-01T10:00:00Z',
  },
  {
    id: 2,
    username: 'manager',
    displayName: 'Manager du bar',
    role: 'MANAGER',
    active: true,
    createdAt: '2026-01-01T09:00:00Z',
  },
];

function router() {
  const instance = createRouter({
    history: createWebHistory(),
    routes: [{ path: '/bar/users', name: 'bar-users', component: UserManagementView }],
  });
  return instance.push('/bar/users').then(() => instance);
}

async function renderView() {
  const instance = await router();
  const utils = render(UserManagementView, { global: { plugins: [instance] } });
  await waitFor(() => expect(usersApi.fetchUsers).toHaveBeenCalled());
  return utils;
}

async function openModalAndFill(overrides: Partial<Record<'displayName' | 'username' | 'password' | 'confirm', string>> = {}) {
  await fireEvent.click(screen.getByRole('button', { name: 'Ajouter un barmaker' }));
  const dialog = await screen.findByRole('dialog');
  await fireEvent.update(within(dialog).getByLabelText(/^Nom affiché/), overrides.displayName ?? 'Alice Martin');
  await fireEvent.update(within(dialog).getByLabelText(/^Nom d’utilisateur/), overrides.username ?? 'alice');
  await fireEvent.update(within(dialog).getByLabelText(/^Mot de passe/), overrides.password ?? 'temporary-pass');
  await fireEvent.update(
    within(dialog).getByLabelText(/^Confirmer le mot de passe/),
    overrides.confirm ?? 'temporary-pass',
  );
  return dialog;
}

describe('manager staff administration view', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    document.body.style.overflow = '';
    setActivePinia(createPinia());
    vi.mocked(usersApi.fetchUsers).mockReset().mockResolvedValue(staff);
    vi.mocked(usersApi.createBarmaker).mockReset();
  });

  it('renders the staff list without any password data', async () => {
    const { container } = await renderView();
    expect(screen.getByText('Barman principal')).toBeTruthy();
    expect(screen.getByText('Manager du bar')).toBeTruthy();
    expect(screen.getByText('@barmaker')).toBeTruthy();
    // No password/hash text ever reaches the DOM.
    expect(container.innerHTML).not.toContain('password');
    expect(container.innerHTML).not.toContain('$2a$');
  });

  it('shows the four creation fields, the fixed role hint and no role selector', async () => {
    await renderView();
    const dialog = await openModalAndFill();
    expect(within(dialog).getByLabelText(/^Nom affiché/)).toBeTruthy();
    expect(within(dialog).getByLabelText(/^Nom d’utilisateur/)).toBeTruthy();
    expect(within(dialog).getByLabelText(/^Mot de passe/)).toBeTruthy();
    expect(within(dialog).getByLabelText(/^Confirmer le mot de passe/)).toBeTruthy();
    expect(within(dialog).getByText('Rôle attribué : Barmaker')).toBeTruthy();
    // No <select> and no role radios/inputs anywhere in the modal.
    expect(within(dialog).queryByRole('combobox')).toBeNull();
    expect(dialog.querySelector('select')).toBeNull();
  });

  it('blocks submission when the password confirmation does not match', async () => {
    await renderView();
    const dialog = await openModalAndFill({ confirm: 'different-pass' });
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Créer le compte' }));
    expect(usersApi.createBarmaker).not.toHaveBeenCalled();
    expect(within(dialog).getByText('Les mots de passe ne correspondent pas.')).toBeTruthy();
  });

  it('blocks submission when the password is too short', async () => {
    await renderView();
    const dialog = await openModalAndFill({ password: 'short', confirm: 'short' });
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Créer le compte' }));
    expect(usersApi.createBarmaker).not.toHaveBeenCalled();
    expect(within(dialog).getByText(/entre 8 et 72 caractères/)).toBeTruthy();
  });

  it('sends only displayName, username and password on a valid submission', async () => {
    vi.mocked(usersApi.createBarmaker).mockResolvedValue({
      id: 3,
      username: 'alice',
      displayName: 'Alice Martin',
      role: 'BARMAKER',
      active: true,
      createdAt: '2026-07-01T12:00:00Z',
    });
    await renderView();
    const dialog = await openModalAndFill();
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Créer le compte' }));

    await waitFor(() => expect(usersApi.createBarmaker).toHaveBeenCalledTimes(1));
    const payload = vi.mocked(usersApi.createBarmaker).mock.calls[0][0];
    expect(payload).toEqual({ displayName: 'Alice Martin', username: 'alice', password: 'temporary-pass' });
    expect(payload).not.toHaveProperty('role');
    expect(payload).not.toHaveProperty('active');
    expect(payload).not.toHaveProperty('passwordConfirmation');
  });

  it('closes the modal, refreshes the list and shows a success toast on success', async () => {
    vi.mocked(usersApi.createBarmaker).mockResolvedValue({
      id: 3,
      username: 'alice',
      displayName: 'Alice Martin',
      role: 'BARMAKER',
      active: true,
      createdAt: '2026-07-01T12:00:00Z',
    });
    await renderView();
    const dialog = await openModalAndFill();
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Créer le compte' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    // Initial load + refresh after creation.
    expect(usersApi.fetchUsers).toHaveBeenCalledTimes(2);
    expect(await screen.findByText('Le compte Barmaker a été créé.')).toBeTruthy();
  });

  it('shows the duplicate-username message on a 409 conflict', async () => {
    vi.mocked(usersApi.createBarmaker).mockRejectedValue(
      new ApiError({ message: 'x', status: 409, code: 'USERNAME_ALREADY_EXISTS' }),
    );
    await renderView();
    const dialog = await openModalAndFill();
    await fireEvent.click(within(dialog).getByRole('button', { name: 'Créer le compte' }));

    expect(await within(dialog).findByText('Ce nom d’utilisateur est déjà utilisé.')).toBeTruthy();
    // Modal stays open so the manager can adjust the username.
    expect(screen.queryByRole('dialog')).not.toBeNull();
  });
});
