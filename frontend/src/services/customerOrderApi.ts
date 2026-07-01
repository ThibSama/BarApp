// Public customer order API: create an order and fetch it for confirmation and
// tracking. Unauthenticated — the order UUID is the only access reference.
import { apiRequest } from './apiClient';
import type { CreateOrderPayload, CustomerOrder } from '@/types/api';

/** `POST /api/orders` — create an anonymous order; returns the persisted order. */
export function createOrder(payload: CreateOrderPayload): Promise<CustomerOrder> {
  return apiRequest<CustomerOrder>('/orders', { method: 'POST', body: payload });
}

/** `GET /api/orders/{orderId}` — load the persisted order for confirm/tracking. */
export function fetchOrder(orderId: string, signal?: AbortSignal): Promise<CustomerOrder> {
  return apiRequest<CustomerOrder>(`/orders/${encodeURIComponent(orderId)}`, { signal });
}
