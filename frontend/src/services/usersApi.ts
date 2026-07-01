// Manager-only staff administration API. Every request is authenticated; the
// generic client attaches the JWT and handles 401 centrally. The payload sent
// to the backend carries only displayName/username/password — never a role, an
// active flag, or the frontend-only password confirmation.
import { apiRequest } from './apiClient';
import type { CreateBarmakerRequest, UserAdminResponse } from '@/types/api';

/** `GET /api/bar/users` — list every staff account (manager-only). */
export function fetchUsers(signal?: AbortSignal): Promise<UserAdminResponse[]> {
  return apiRequest<UserAdminResponse[]>('/bar/users', { auth: true, signal });
}

/** `POST /api/bar/users` — create a new barmaker (manager-only). */
export function createBarmaker(payload: CreateBarmakerRequest): Promise<UserAdminResponse> {
  const body: CreateBarmakerRequest = {
    displayName: payload.displayName,
    username: payload.username,
    password: payload.password,
  };
  return apiRequest<UserAdminResponse>('/bar/users', { method: 'POST', auth: true, body });
}
