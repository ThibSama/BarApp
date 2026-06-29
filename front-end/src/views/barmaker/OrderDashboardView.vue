<script setup lang="ts">
import OrderSummaryCard from '@/components/barmaker/OrderSummaryCard.vue';
import { useBarmakerStore } from '@/stores/barmaker';
import { orderStatusLabels } from '@/utils/formatters';
const barmaker = useBarmakerStore();
</script>

<template>
  <section class="stack">
    <div class="page-title"><div><p class="eyebrow">Espace barmaker</p><h1>Tableau des commandes</h1></div></div>
    <label class="card filter-inline">Filtrer les commandes<select v-model="barmaker.selectedStatus"><option value="all">Tous les statuts</option><option value="ordered">{{ orderStatusLabels.ordered }}</option><option value="preparing">{{ orderStatusLabels.preparing }}</option><option value="completed">{{ orderStatusLabels.completed }}</option></select></label>
    <div v-if="barmaker.filteredOrders.length" class="stack"><OrderSummaryCard v-for="order in barmaker.filteredOrders" :key="order.id" :order="order" /></div>
    <section v-else class="card empty-state"><h2>Aucune commande dans ce statut</h2><p>Changez de filtre pour consulter les autres commandes.</p></section>
  </section>
</template>
