<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import ProgressSteps from '@/components/client/ProgressSteps.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useOrderStore } from '@/stores/orders';
import { formatCurrency, orderStatusLabels, preparationStepLabels } from '@/utils/formatters';
import { getPaymentMethodLabel } from '@/utils/payments';
const route = useRoute();
const orders = useOrderStore();
const order = computed(() => orders.getOrderById(String(route.params.orderId)));
</script>

<template>
  <section v-if="order" class="stack">
    <RouterLink to="/barmaker/commandes">← Retour aux commandes</RouterLink>
    <div class="page-title"><div><p class="eyebrow">Détail commande</p><h1>{{ order.orderNumber }}</h1><p>{{ formatCurrency(order.total) }}</p></div><StatusBadge :label="orderStatusLabels[order.status]" :tone="order.status === 'completed' ? 'success' : 'warning'" /></div>
    <div class="card payment-info"><strong>Mode de paiement</strong><span>{{ getPaymentMethodLabel(order.paymentMethod) }}</span></div>
    <article v-for="item in order.items" :key="item.id" class="card prep-card">
      <div><h2>{{ item.quantity }} × {{ item.cocktailName }}</h2><p>Taille {{ item.size }} · {{ preparationStepLabels[item.preparationStep] }}</p><ProgressSteps :current-step="item.preparationStep" /></div>
      <button class="button" type="button" :disabled="item.preparationStep === 'completed'" @click="orders.advanceItem(order.id, item.id)">{{ item.preparationStep === 'completed' ? 'Cocktail terminé' : 'Étape suivante' }}</button>
    </article>
    <p v-if="order.status === 'completed'" class="alert success">Tous les cocktails sont terminés. La commande est automatiquement marquée comme terminée.</p>
  </section>
  <section v-else class="card empty-state"><h1>Commande introuvable</h1><RouterLink class="button" to="/barmaker/commandes">Retour aux commandes</RouterLink></section>
</template>

<style scoped>
.prep-card { display: grid; gap: 1rem; align-items: center; }
.payment-info { display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
@media (min-width: 800px) { .prep-card { grid-template-columns: 1fr auto; } }
</style>
