<script setup lang="ts">
import type { Order } from '@/types/domain';
import { formatCurrency, formatTime, orderStatusLabels } from '@/utils/formatters';
import StatusBadge from '@/components/common/StatusBadge.vue';

function toneFor(status: Order['status']) {
  if (status === 'completed') return 'success' as const;
  if (status === 'preparing') return 'warning' as const;
  return 'neutral' as const;
}

defineProps<{ order: Order }>();
</script>

<template>
  <article class="card order-card">
    <div>
      <p class="eyebrow">{{ formatTime(order.createdAt) }}</p>
      <h3>{{ order.orderNumber }}</h3>
    </div>
    <StatusBadge :label="orderStatusLabels[order.status]" :tone="toneFor(order.status)" />
    <p>{{ order.items.reduce((total, item) => total + item.quantity, 0) }} cocktail(s)</p>
    <strong>{{ formatCurrency(order.total) }}</strong>
    <RouterLink class="button secondary" :to="`/barmaker/commandes/${order.id}`">Ouvrir la commande</RouterLink>
  </article>
</template>

<style scoped>
.order-card { display: grid; gap: 0.75rem; align-items: start; }
h3 { margin: 0; }
@media (min-width: 760px) { .order-card { grid-template-columns: 1.2fr auto auto auto auto; align-items: center; } }
</style>
