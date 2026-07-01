<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useCustomerOrderStore } from '@/stores/customerOrder';
import { apiOrderStatusLabels, apiOrderStatusTone, formatCurrency } from '@/utils/formatters';
import { getPaymentMethodLabel, getPaymentSimulationMessage } from '@/utils/payments';

const route = useRoute();
const orders = useCustomerOrderStore();
const orderId = computed(() => String(route.params.orderId));
const order = computed(() => orders.order);

// Reload-safe: always fetch the persisted order by id rather than relying on the
// POST response or navigation state.
onMounted(() => orders.loadOrder(orderId.value, { initial: true }));
</script>

<template>
  <section v-if="orders.loading && !order" class="card empty-state" aria-busy="true"><h1>Chargement de la commande…</h1></section>

  <section v-else-if="orders.notFound" class="card empty-state"><h1>Commande introuvable</h1><RouterLink class="button" :to="{ name: 'client-menu' }">Retour à la carte</RouterLink></section>

  <section v-else-if="!order && orders.error" class="card empty-state">
    <h1>Impossible de charger la commande</h1>
    <p>{{ orders.error }}</p>
    <button class="button" type="button" @click="orders.loadOrder(orderId, { initial: true })">Réessayer</button>
  </section>

  <section v-else-if="order" class="card empty-state confirmation">
    <p class="eyebrow">Commande confirmée</p>
    <h1>Merci, votre commande {{ order.publicCode }} est enregistrée.</h1>
    <p>Le barmaker va commencer la préparation. Vous pouvez suivre l’avancement en temps réel.</p>
    <div class="meta-row">
      <span><strong>Table :</strong> {{ order.tableNumber }}</span>
      <StatusBadge :label="apiOrderStatusLabels[order.status]" :tone="apiOrderStatusTone(order.status)" />
    </div>
    <ul class="summary-list"><li v-for="item in order.items" :key="item.id">{{ item.cocktailName }} · taille {{ item.size }}</li></ul>
    <p><strong>Mode de paiement :</strong> {{ getPaymentMethodLabel(order.paymentMethod) }}</p>
    <p class="alert success">{{ getPaymentSimulationMessage(order.paymentMethod) }}</p>
    <strong>Total : {{ formatCurrency(order.totalAmount) }}</strong>
    <RouterLink class="button" :to="{ name: 'client-order-tracking', params: { orderId: order.id } }">Suivre la commande</RouterLink>
  </section>

  <section v-else class="card empty-state"><h1>Commande introuvable</h1><RouterLink class="button" :to="{ name: 'client-menu' }">Retour à la carte</RouterLink></section>
</template>

<style scoped>
.confirmation { gap: var(--space-3); }
.meta-row { display: flex; align-items: center; justify-content: center; gap: var(--space-4); flex-wrap: wrap; }
.summary-list { list-style: none; padding: 0; margin: 0; display: grid; gap: var(--space-1); color: var(--color-text-secondary); }
</style>
