<script setup lang="ts">
import AppIcon from '@/components/common/AppIcon.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { BarOrderSummary } from '@/types/api';
import { apiOrderStatusLabels, apiOrderStatusTone, formatCurrency, formatTime } from '@/utils/formatters';

defineProps<{ order: BarOrderSummary }>();
</script>

<template>
  <article class="order-row">
    <div class="order-main">
      <p class="eyebrow">{{ formatTime(order.createdAt) }}</p>
      <h3>{{ order.publicCode }}</h3>
    </div>
    <StatusBadge :label="apiOrderStatusLabels[order.status]" :tone="apiOrderStatusTone(order.status)" />
    <p class="meta">{{ order.completedItemCount }}/{{ order.itemCount }} cocktail(s)</p>
    <strong class="total">{{ formatCurrency(order.totalAmount) }}</strong>
    <RouterLink class="open-link" :to="`/bar/orders/${order.id}`">Ouvrir <AppIcon name="chevron-right" :size="17" /></RouterLink>
  </article>
</template>

<style scoped>
.order-row { position: relative; display: grid; gap: var(--space-3); align-items: center; padding: 20px 22px 20px 26px; border-bottom: 1px solid var(--color-border); background: #fff; }
.order-row::before { content: ''; position: absolute; left: 0; top: 18px; bottom: 18px; width: 3px; border-radius: var(--radius-round); background: var(--color-accent); opacity: 0.72; }
.order-row:last-child { border-bottom: 0; }
.order-main { min-width: 0; }
h3 { margin: 0; font-size: 1.12rem; letter-spacing: -0.02em; }
.meta { margin: 0; color: var(--color-text-secondary); font-size: 0.94rem; font-weight: 600; }
.total { font-size: 1rem; color: var(--color-primary); }
.open-link { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-1); min-height: 40px; padding: 0 14px; border-radius: var(--radius-medium); background: var(--color-primary); color: #fff; font-weight: 800; font-size: 0.9rem; box-shadow: 0 6px 14px rgba(29,43,31,0.1); }
.open-link:hover { background: var(--color-primary-hover); text-decoration: none; }
@media (min-width: 900px) { .order-row { grid-template-columns: minmax(150px, 1.2fr) auto minmax(130px, auto) auto auto; } }
@media (max-width: 640px) { .order-row { border: 1px solid var(--color-border); border-radius: var(--radius-large); margin: var(--space-3); } .open-link { width: 100%; } }
</style>
