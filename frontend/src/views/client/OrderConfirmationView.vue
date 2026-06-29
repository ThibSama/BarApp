<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/orders';
import { formatCurrency } from '@/utils/formatters';
import { getPaymentMethodLabel, getPaymentSimulationMessage } from '@/utils/payments';
const route = useRoute();
const orders = useOrderStore();
const order = computed(() => orders.getOrderById(String(route.params.orderId)));
</script>

<template>
  <section v-if="order" class="card empty-state">
    <p class="eyebrow">Commande confirmée</p>
    <h1>Merci, votre commande {{ order.orderNumber }} est enregistrée.</h1>
    <p>Le barmaker va commencer la préparation. Vous pouvez suivre l’avancement en temps réel dans le prototype.</p>
    <ul class="summary-list"><li v-for="item in order.items" :key="item.id">{{ item.quantity }} × {{ item.cocktailName }} taille {{ item.size }}</li></ul>
    <p><strong>Mode de paiement :</strong> {{ getPaymentMethodLabel(order.paymentMethod) }}</p>
    <p class="alert success">{{ getPaymentSimulationMessage(order.paymentMethod) }}</p>
    <strong>Total : {{ formatCurrency(order.total) }}</strong>
    <RouterLink class="button" :to="`/client/suivi/${order.id}`">Suivre la commande</RouterLink>
  </section>
  <section v-else class="card empty-state"><h1>Commande introuvable</h1><RouterLink class="button" to="/client/menu">Retour à la carte</RouterLink></section>
</template>
