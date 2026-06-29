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
const order = computed(() => orders.getOrderById(String(route.params.orderId)));
</script>

<template>
  <section v-if="order" class="stack">
    <div class="page-title"><div><p class="eyebrow">Suivi de commande</p><h1>{{ order.orderNumber }}</h1></div><StatusBadge :label="orderStatusLabels[order.status]" :tone="order.status === 'completed' ? 'success' : 'warning'" /></div>
    <div class="card info-row"><strong>Mode de paiement</strong><span>{{ getPaymentMethodLabel(order.paymentMethod) }}</span></div>
    <div class="card global-progress"><span :class="{ active: true }">Commandée</span><span :class="{ active: order.status !== 'ordered' }">En cours de préparation</span><span :class="{ active: order.status === 'completed' }">Terminée</span></div>
    <div class="card-grid two">
      <article v-for="item in order.items" :key="item.id" class="card"><h2>{{ item.quantity }} × {{ item.cocktailName }}</h2><p>Taille {{ item.size }} · {{ preparationStepLabels[item.preparationStep] }}</p><ProgressSteps :current-step="item.preparationStep" /></article>
    </div>
  </section>
  <section v-else class="card empty-state"><h1>Commande introuvable</h1><RouterLink class="button" to="/client/menu">Retour à la carte</RouterLink></section>
</template>

<style scoped>
.global-progress { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.5rem; text-align: center; }
.global-progress span { padding: 0.75rem; border-radius: 0.75rem; background: #f1f5f9; color: #64748b; }
.global-progress .active { background: #dbeafe; color: #1d4ed8; font-weight: 800; }
.info-row { display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
</style>
