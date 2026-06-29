import { mockOrders } from '@/mocks/orders';
import type { Order, PaymentMethod } from '@/types/domain';
import { loadState, saveState } from './localPersistence';

const ordersKey = 'barapp.orders';
const defaultPaymentMethod: PaymentMethod = 'card_at_counter';

function migrateOrder(order: Order | (Omit<Order, 'paymentMethod'> & { paymentMethod?: PaymentMethod })): Order {
  return { ...order, paymentMethod: order.paymentMethod ?? defaultPaymentMethod };
}

export function loadOrders(): Order[] {
  return loadState(ordersKey, mockOrders).map(migrateOrder);
}

export function saveOrders(orders: Order[]): void {
  saveState(ordersKey, orders);
}
