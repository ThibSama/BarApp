// Protected barmaker order API calls. Every request is authenticated.
import { apiRequest } from './apiClient';
import type { BarOrderDetail, BarOrderSummary } from '@/types/api';

/** `GET /api/bar/orders?completed=<bool>` — active or completed summaries. */
export function fetchOrderSummaries(
  completed: boolean,
  signal?: AbortSignal,
): Promise<BarOrderSummary[]> {
  return apiRequest<BarOrderSummary[]>(`/bar/orders?completed=${completed}`, {
    auth: true,
    signal,
  });
}

/** `GET /api/bar/orders/{orderId}` — full order detail. */
export function fetchOrderDetail(orderId: string, signal?: AbortSignal): Promise<BarOrderDetail> {
  return apiRequest<BarOrderDetail>(`/bar/orders/${encodeURIComponent(orderId)}`, {
    auth: true,
    signal,
  });
}

/**
 * `PATCH /api/bar/order-items/{itemId}/next-step` — advance one cocktail by
 * exactly one step. Returns the refreshed parent order. No request body.
 */
export function advanceOrderItem(itemId: string): Promise<BarOrderDetail> {
  return apiRequest<BarOrderDetail>(
    `/bar/order-items/${encodeURIComponent(itemId)}/next-step`,
    { method: 'PATCH', auth: true },
  );
}
