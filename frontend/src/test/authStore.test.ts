import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/services/apiClient';
import { useAuthStore } from '@/stores/auth';
import * as authApi from '@/services/authApi';
import type { AuthenticatedUser } from '@/types/api';

vi.mock('@/services/authApi', () => ({
  login: vi.fn(),
  fetchCurrentUser: vi.fn(),
}));

const TOKEN_KEY = 'barapp.auth.token';
const EXPIRES_KEY = 'barapp.auth.expiresAt';

const barmaker: AuthenticatedUser = {
  id: 1,
  username: 'barmaker',
  displayName: 'Barman principal',
  role: 'BARMAKER',
};

const manager: AuthenticatedUser = {
  id: 2,
  username: 'manager',
  displayName: 'Manager du bar',
  role: 'MANAGER',
};

function storageDump(): string {
  let dump = '';
  for (let i = 0; i < sessionStorage.length; i += 1) {
    const key = sessionStorage.key(i)!;
    dump += `${key}=${sessionStorage.getItem(key)};`;
  }
  return dump;
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.mocked(authApi.login).mockReset();
    vi.mocked(authApi.fetchCurrentUser).mockReset();
  });

  it('stores the token in sessionStorage on successful login without persisting the password', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'tok-abc',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: barmaker,
    });
    const auth = useAuthStore();

    const ok = await auth.login('barmaker', 'super-secret');

    expect(ok).toBe(true);
    expect(auth.isAuthenticated).toBe(true);
    expect(sessionStorage.getItem(TOKEN_KEY)).toBe('tok-abc');
    expect(storageDump()).not.toContain('super-secret');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('accepts a BARMAKER login and reports isManager false', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'tok-barmaker',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: barmaker,
    });
    const auth = useAuthStore();

    const ok = await auth.login('barmaker', 'super-secret');

    expect(ok).toBe(true);
    expect(auth.isAuthenticated).toBe(true);
    expect(auth.isManager).toBe(false);
    expect(auth.user?.role).toBe('BARMAKER');
  });

  it('accepts a MANAGER login and reports isManager true', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'tok-manager',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: manager,
    });
    const auth = useAuthStore();

    const ok = await auth.login('manager', 'super-secret');

    expect(ok).toBe(true);
    expect(auth.isAuthenticated).toBe(true);
    expect(auth.isManager).toBe(true);
    expect(auth.user?.role).toBe('MANAGER');
  });

  it('rejects a login whose role is unknown and clears the session', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'tok-bad',
      tokenType: 'Bearer',
      expiresIn: 3600,
      // Simulate a malformed/unknown role coming from the backend.
      user: { ...barmaker, role: 'ADMIN' as unknown as AuthenticatedUser['role'] },
    });
    const auth = useAuthStore();

    const ok = await auth.login('barmaker', 'super-secret');

    expect(ok).toBe(false);
    expect(auth.isAuthenticated).toBe(false);
    expect(auth.accessToken).toBeNull();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('clears the session when the restored profile has an unknown role', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'tok');
    sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + 100_000));
    vi.mocked(authApi.fetchCurrentUser).mockResolvedValue({
      ...barmaker,
      role: 'ROOT' as unknown as AuthenticatedUser['role'],
    });
    const auth = useAuthStore();

    const ok = await auth.ensureSession();

    expect(ok).toBe(false);
    expect(auth.accessToken).toBeNull();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('restores a MANAGER session and keeps isManager true', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'tok');
    sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + 100_000));
    vi.mocked(authApi.fetchCurrentUser).mockResolvedValue(manager);
    const auth = useAuthStore();

    const ok = await auth.ensureSession();

    expect(ok).toBe(true);
    expect(auth.isManager).toBe(true);
  });

  it('exposes a generic French error and stores nothing on failed login', async () => {
    vi.mocked(authApi.login).mockRejectedValue(
      new ApiError({ message: 'backend detail', status: 401, code: 'INVALID_CREDENTIALS' }),
    );
    const auth = useAuthStore();

    const ok = await auth.login('barmaker', 'wrong');

    expect(ok).toBe(false);
    expect(auth.error).toBe('Identifiants invalides. Veuillez réessayer.');
    expect(auth.accessToken).toBeNull();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('clears a locally expired session without calling the backend', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'old-token');
    sessionStorage.setItem(EXPIRES_KEY, String(Date.now() - 1000));
    const auth = useAuthStore();

    const ok = await auth.ensureSession();

    expect(ok).toBe(false);
    expect(auth.accessToken).toBeNull();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(authApi.fetchCurrentUser).not.toHaveBeenCalled();
  });

  it('restores the session when /api/auth/me succeeds', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'tok');
    sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + 100_000));
    vi.mocked(authApi.fetchCurrentUser).mockResolvedValue(barmaker);
    const auth = useAuthStore();

    const ok = await auth.ensureSession();

    expect(ok).toBe(true);
    expect(auth.isAuthenticated).toBe(true);
    expect(auth.displayName).toBe('Barman principal');
  });

  it('shares a single /api/auth/me call across concurrent restores', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'tok');
    sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + 100_000));
    vi.mocked(authApi.fetchCurrentUser).mockResolvedValue(barmaker);
    const auth = useAuthStore();

    const [a, b] = await Promise.all([auth.ensureSession(), auth.ensureSession()]);

    expect(a).toBe(true);
    expect(b).toBe(true);
    expect(authApi.fetchCurrentUser).toHaveBeenCalledTimes(1);
  });

  it('clears the session when /api/auth/me fails', async () => {
    sessionStorage.setItem(TOKEN_KEY, 'tok');
    sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + 100_000));
    vi.mocked(authApi.fetchCurrentUser).mockRejectedValue(new ApiError({ message: 'x', status: 401 }));
    const auth = useAuthStore();

    const ok = await auth.ensureSession();

    expect(ok).toBe(false);
    expect(auth.accessToken).toBeNull();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('removes all session data on logout', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      accessToken: 'tok-abc',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: barmaker,
    });
    const auth = useAuthStore();
    await auth.login('barmaker', 'secret');

    auth.logout();

    expect(auth.isAuthenticated).toBe(false);
    expect(auth.user).toBeNull();
    expect(sessionStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(storageDump()).toBe('');
  });
});
