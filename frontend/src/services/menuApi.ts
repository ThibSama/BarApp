// Public customer menu API. Unauthenticated; no token is ever attached.
import { apiRequest } from './apiClient';
import type { MenuResponse } from '@/types/api';

/** `GET /api/menu` — the full active catalogue grouped by category. */
export function fetchMenu(signal?: AbortSignal): Promise<MenuResponse> {
  return apiRequest<MenuResponse>('/menu', { signal });
}
