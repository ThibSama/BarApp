// Authentication API calls. Thin wrappers over the generic client so the auth
// store stays focused on state, and tests can mock at the service boundary.
import { apiRequest } from './apiClient';
import type { AuthenticatedUser, LoginResponse } from '@/types/api';

/** `POST /api/auth/login` — exchange credentials for a JWT and profile. */
export function login(username: string, password: string): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', {
    method: 'POST',
    body: { username, password },
  });
}

/** `GET /api/auth/me` — validate the current token and return the profile. */
export function fetchCurrentUser(): Promise<AuthenticatedUser> {
  return apiRequest<AuthenticatedUser>('/auth/me', { auth: true });
}
