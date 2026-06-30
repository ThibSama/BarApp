<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import ProgressSteps from '@/components/client/ProgressSteps.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useOrderStore } from '@/stores/orders';
import { orderStatusLabels, preparationStepLabels } from '@/utils/formatters';
import { getPaymentMethodLabel } from '@/utils/payments';
const route = useRoute();
const orders = useOrderStore();
const order = computed(() => {
  const routeOrderId = route.params.orderId ? String(route.params.orderId) : '';
  return routeOrderId ? orders.getOrderById(routeOrderId) : orders.activeOrders[0];
});
</script>

<template>
  <section v-if="order" class="stack">
    <div class="page-title"><div><p class="eyebrow">Suivi de commande</p><h1>{{ order.orderNumber }}</h1><div class="title-underline" aria-hidden="true"></div></div><StatusBadge :label="orderStatusLabels[order.status]" :tone="order.status === 'completed' ? 'success' : 'warning'" /></div>
    <div class="card info-row"><strong>Mode de paiement</strong><span>{{ getPaymentMethodLabel(order.paymentMethod) }}</span></div>
    <ol class="card global-progress" aria-label="Avancement global de la commande"><li class="completed">Commandée</li><li :class="{ active: order.status === 'preparing', completed: order.status === 'completed' }">En préparation</li><li :class="{ active: order.status === 'completed' }">Terminée</li></ol>
    <div class="card-grid two">
      <article v-for="item in order.items" :key="item.id" class="card"><h2>{{ item.quantity }} × {{ item.cocktailName }}</h2><p>Taille {{ item.size }} · {{ preparationStepLabels[item.preparationStep] }}</p><ProgressSteps :current-step="item.preparationStep" /></article>
    </div>
  </section>
  <section v-else class="card empty-state"><h1>Aucune commande en cours</h1><p>Votre commande apparaîtra ici après sa validation.</p><RouterLink class="button" to="/client/menu">Retour à la carte</RouterLink></section>
</template>

<style scoped>
.global-progress { list-style: none; display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-2); text-align: center; padding: var(--space-4); }
.global-progress li { padding: var(--space-3); border-radius: var(--radius-medium); background: var(--color-surface-muted); color: var(--color-text-secondary); font-weight: 800; }
.global-progress .completed { background: #e8f3e9; color: var(--color-success); }
.global-progress .active { background: #fff6dd; color: #7b5300; box-shadow: inset 0 0 0 2px rgba(212,175,55,0.32); }
.info-row { display: flex; justify-content: space-between; gap: var(--space-4); flex-wrap: wrap; }
@media (max-width: 520px) { .global-progress { grid-template-columns: 1fr; text-align: left; } }
</style>
