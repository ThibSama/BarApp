import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import type { OrderStatus } from '@/types/domain';
import { useOrderStore } from './orders';

export const useBarmakerStore = defineStore('barmaker', () => {
  const selectedStatus = ref<OrderStatus | 'all'>('all');
  const selectedCategoryId = ref<string>('all');
  const ordersStore = useOrderStore();

  const filteredOrders = computed(() => selectedStatus.value === 'all' ? ordersStore.orders : ordersStore.orders.filter((order) => order.status === selectedStatus.value));

  return { selectedStatus, selectedCategoryId, filteredOrders };
});
