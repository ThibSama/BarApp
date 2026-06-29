import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import { loadOrders, saveOrders } from '@/services/orderService';
import { useCartStore } from './cart';
import { useCatalogStore } from './catalog';
import type { Order, OrderItem, PaymentMethod, PreparationStep } from '@/types/domain';
import { calculateOrderTotal } from '@/utils/pricing';
import { preparationSteps } from '@/utils/formatters';

function createOrderNumber(): string {
  return `BA-${Math.floor(1000 + Math.random() * 9000)}`;
}

export const useOrderStore = defineStore('orders', () => {
  const orders = ref<Order[]>(loadOrders());
  const lastCreatedOrderId = ref<string>('');
  watch(orders, (value) => saveOrders(value), { deep: true });

  const activeOrders = computed(() => orders.value.filter((order) => order.status !== 'completed'));
  const completedOrders = computed(() => orders.value.filter((order) => order.status === 'completed'));

  function getOrderById(id: string): Order | undefined {
    return orders.value.find((order) => order.id === id);
  }

  function createOrderFromCart(paymentMethod: PaymentMethod): Order | null {
    const cart = useCartStore();
    const catalog = useCatalogStore();
    if (cart.items.length === 0) return null;
    const items: OrderItem[] = cart.items.map((item) => {
      const cocktail = catalog.getCocktailById(item.cocktailId);
      return {
        id: `item-${Date.now()}-${Math.random().toString(16).slice(2)}`,
        cocktailId: item.cocktailId,
        cocktailName: cocktail?.name ?? 'Cocktail indisponible',
        size: item.size,
        quantity: item.quantity,
        unitPrice: cocktail?.prices[item.size] ?? 0,
        preparationStep: 'ingredients',
      };
    });
    const order: Order = { id: `order-${Date.now()}`, orderNumber: createOrderNumber(), createdAt: new Date().toISOString(), status: 'ordered', paymentMethod, items, total: calculateOrderTotal(items) };
    orders.value.unshift(order);
    lastCreatedOrderId.value = order.id;
    cart.clearCart();
    return order;
  }

  function advanceItem(orderId: string, itemId: string): void {
    const order = getOrderById(orderId);
    if (!order) return;
    const item = order.items.find((entry) => entry.id === itemId);
    if (!item || item.preparationStep === 'completed') return;
    const currentIndex = preparationSteps.indexOf(item.preparationStep);
    item.preparationStep = preparationSteps[currentIndex + 1] as PreparationStep;
    if (order.status === 'ordered') order.status = 'preparing';
    updateOrderStatus(orderId);
  }

  function updateOrderStatus(orderId: string): void {
    const order = getOrderById(orderId);
    if (!order) return;
    if (order.items.every((item) => item.preparationStep === 'completed')) order.status = 'completed';
    else if (order.items.some((item) => item.preparationStep !== 'ingredients')) order.status = 'preparing';
  }

  return { orders, activeOrders, completedOrders, lastCreatedOrderId, getOrderById, createOrderFromCart, advanceItem, updateOrderStatus };
});
