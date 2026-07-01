import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { configureApiAuth } from '@/services/apiClient';
import { fetchCurrentUser, login as loginRequest } from '@/services/authApi';
import type { AuthenticatedUser, UserRole } from '@/types/api';

// Session-scoped storage: the JWT lives only for the browser session and is
// dropped when the tab closes. We deliberately do NOT use localStorage.
const TOKEN_KEY = 'barapp.auth.token';
const EXPIRES_KEY = 'barapp.auth.expiresAt';
const USER_KEY = 'barapp.auth.user';

const GENERIC_LOGIN_ERROR = 'Identifiants invalides. Veuillez réessayer.';

// The only two authenticated staff roles. Any other value (missing, misspelled,
// an arbitrary string, or a removed legacy role) is rejected so a malformed
// profile can never grant access.
const VALID_ROLES: readonly UserRole[] = ['BARMAKER', 'MANAGER'];

function isValidUser(value: unknown): value is AuthenticatedUser {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Partial<AuthenticatedUser>;
  return (
    typeof candidate.username === 'string' &&
    candidate.role !== undefined &&
    VALID_ROLES.includes(candidate.role)
  );
}

function readStoredUser(): AuthenticatedUser | null {
  const raw = sessionStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    const parsed: unknown = JSON.parse(raw);
    if (isValidUser(parsed)) return parsed;
  } catch {
    /* ignore malformed storage */
  }
  return null;
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(sessionStorage.getItem(TOKEN_KEY));
  const expiresAt = ref<number>(Number(sessionStorage.getItem(EXPIRES_KEY)) || 0);
  const user = ref<AuthenticatedUser | null>(readStoredUser());
  // True once /api/auth/me has confirmed the token in this app lifecycle.
  const sessionValidated = ref(false);
  const loading = ref(false);
  const error = ref('');

  // Shared in-flight promise so concurrent navigations trigger a single
  // /api/auth/me call during startup.
  let restorePromise: Promise<boolean> | null = null;

  const isAuthenticated = computed(() => Boolean(accessToken.value) && sessionValidated.value);
  const isManager = computed(() => user.value?.role === 'MANAGER');
  const displayName = computed(() => user.value?.displayName ?? '');

  function isLocallyExpired(): boolean {
    return expiresAt.value > 0 && Date.now() >= expiresAt.value;
  }

  function persist(): void {
    if (accessToken.value) sessionStorage.setItem(TOKEN_KEY, accessToken.value);
    else sessionStorage.removeItem(TOKEN_KEY);
    if (expiresAt.value) sessionStorage.setItem(EXPIRES_KEY, String(expiresAt.value));
    else sessionStorage.removeItem(EXPIRES_KEY);
    if (user.value) sessionStorage.setItem(USER_KEY, JSON.stringify(user.value));
    else sessionStorage.removeItem(USER_KEY);
  }

  function clearSession(): void {
    accessToken.value = null;
    expiresAt.value = 0;
    user.value = null;
    sessionValidated.value = false;
    restorePromise = null;
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_KEY);
    sessionStorage.removeItem(USER_KEY);
  }

  // The HTTP client reads the token and reacts to 401 through these hooks,
  // keeping it free of any store/router import (no circular dependency).
  configureApiAuth({
    getToken: () => accessToken.value,
    onUnauthorized: () => clearSession(),
  });

  async function login(username: string, password: string): Promise<boolean> {
    loading.value = true;
    error.value = '';
    try {
      const response = await loginRequest(username, password);
      if (!isValidUser(response.user)) {
        clearSession();
        error.value = GENERIC_LOGIN_ERROR;
        return false;
      }
      accessToken.value = response.accessToken;
      expiresAt.value = Date.now() + response.expiresIn * 1000;
      user.value = response.user;
      sessionValidated.value = true;
      persist();
      return true;
    } catch (err) {
      clearSession();
      // Surface a single generic French message; never leak technical detail.
      error.value = GENERIC_LOGIN_ERROR;
      void err;
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function runRestore(): Promise<boolean> {
    if (!accessToken.value || isLocallyExpired()) {
      clearSession();
      return false;
    }
    try {
      const profile = await fetchCurrentUser();
      if (!isValidUser(profile)) {
        clearSession();
        return false;
      }
      user.value = profile;
      sessionValidated.value = true;
      persist();
      return true;
    } catch {
      // Any failure (401, malformed profile, network) drops the session so a
      // rejected or unverifiable token never grants access.
      clearSession();
      return false;
    }
  }

  /**
   * Validate the current session, sharing one in-flight /api/auth/me call across
   * simultaneous callers. Returns whether a valid barmaker session exists.
   */
  async function ensureSession(): Promise<boolean> {
    if (sessionValidated.value) return true;
    if (!accessToken.value) return false;
    if (!restorePromise) {
      restorePromise = runRestore().finally(() => {
        restorePromise = null;
      });
    }
    return restorePromise;
  }

  function logout(): void {
    // No backend revocation endpoint exists; logout is local token deletion.
    clearSession();
  }

  return {
    accessToken,
    expiresAt,
    user,
    sessionValidated,
    loading,
    error,
    isAuthenticated,
    isManager,
    displayName,
    login,
    ensureSession,
    logout,
    clearSession,
  };
});
