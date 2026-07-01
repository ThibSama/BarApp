<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import BarPreparationProgress from '@/components/barmaker/BarPreparationProgress.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useCustomerOrderStore } from '@/stores/customerOrder';
import { usePolling } from '@/composables/usePolling';
import { apiOrderStatusLabels, apiOrderStatusTone, apiPreparationStatusLabels } from '@/utils/formatters';
import { getPaymentMethodLabel } from '@/utils/payments';

const route = useRoute();
const orders = useCustomerOrderStore();

// The order id comes from the route; fall back to the last order created in this
// session (no customer account exists, so there is no persistent history).
const orderId = computed(() => (route.params.orderId ? String(route.params.orderId) : orders.lastOrderId));
const order = computed(() => orders.order);

onMounted(() => {
  if (orderId.value) void orders.loadOrder(orderId.value, { initial: true });
});

// Poll ~2.5s; the usePolling helper skips while hidden and refreshes on return.
usePolling(() => {
  if (!orderId.value) return;
  if (order.value?.status === 'COMPLETED') return; // stop once fully completed
  void orders.loadOrder(orderId.value, { initial: false });
}, 2500);
</script>

<template>
  <section v-if="orders.loading && !order" class="card empty-state" aria-busy="true"><h1>Chargement du suivi…</h1></section>

  <section v-else-if="orders.notFound" class="card empty-state"><h1>Commande introuvable</h1><RouterLink class="button" :to="{ name: 'client-menu' }">Retour à la carte</RouterLink></section>

  <section v-else-if="!order && orders.error" class="card empty-state">
    <h1>Suivi indisponible</h1>
    <p>{{ orders.error }}</p>
    <button v-if="orderId" class="button" type="button" @click="orders.loadOrder(orderId, { initial: true })">Réessayer</button>
  </section>

  <section v-else-if="order" class="stack">
    <div class="page-title"><div><p class="eyebrow">Suivi de commande</p><h1>{{ order.publicCode }}</h1><div class="title-underline" aria-hidden="true"></div></div><StatusBadge :label="apiOrderStatusLabels[order.status]" :tone="apiOrderStatusTone(order.status)" /></div>
    <div class="card info-row"><span><strong>Table</strong> {{ order.tableNumber }}</span><span><strong>Paiement</strong> {{ getPaymentMethodLabel(order.paymentMethod) }}</span></div>
    <ol class="card global-progress" aria-label="Avancement global de la commande"><li class="completed">Commandée</li><li :class="{ active: order.status === 'IN_PROGRESS', completed: order.status === 'COMPLETED' }">En préparation</li><li :class="{ active: order.status === 'COMPLETED' }">Terminée</li></ol>
    <div class="card-grid two">
      <article v-for="item in order.items" :key="item.id" class="card"><h2>{{ item.cocktailName }}</h2><p>Taille {{ item.size }} · {{ apiPreparationStatusLabels[item.preparationStatus] }}</p><BarPreparationProgress :current-step="item.preparationStatus" /></article>
    </div>
  </section>

  <section v-else class="card empty-state"><h1>Aucune commande en cours</h1><p>Votre commande apparaîtra ici après sa validation.</p><RouterLink class="button" :to="{ name: 'client-menu' }">Retour à la carte</RouterLink></section>
</template>

<style scoped>
.global-progress { list-style: none; display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-2); text-align: center; padding: var(--space-4); }
.global-progress li { padding: var(--space-3); border-radius: var(--radius-medium); background: var(--color-surface-muted); color: var(--color-text-secondary); font-weight: 800; }
.global-progress .completed { background: #e8f3e9; color: var(--color-success); }
.global-progress .active { background: #fff6dd; color: #7b5300; box-shadow: inset 0 0 0 2px rgba(212,175,55,0.32); }
.info-row { display: flex; justify-content: space-between; gap: var(--space-4); flex-wrap: wrap; }
.info-row span { display: inline-flex; gap: var(--space-2); }
@media (max-width: 520px) { .global-progress { grid-template-columns: 1fr; text-align: left; } }
</style>
