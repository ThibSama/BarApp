<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import BarmakerPageHeader from "@/components/barmaker/BarmakerPageHeader.vue";
import OrderSummaryCard from "@/components/barmaker/OrderSummaryCard.vue";
import { useBarmakerOrderStore } from "@/stores/barmakerOrders";
import { usePolling } from "@/composables/usePolling";

type Tab = "active" | "completed";

const store = useBarmakerOrderStore();
const tab = ref<Tab>("active");

const orders = computed(() =>
  tab.value === "active" ? store.activeSummaries : store.completedSummaries,
);
const loading = computed(() =>
  tab.value === "active" ? store.loadingActive : store.loadingCompleted,
);

function refreshCurrent(initial = false): void {
  if (tab.value === "active") void store.loadActive({ initial });
  else void store.loadCompleted({ initial });
}

function selectTab(next: Tab): void {
  if (tab.value === next) return;
  tab.value = next;
  refreshCurrent(true);
}

onMounted(() => store.loadActive({ initial: true }));

// Poll only the currently displayed queue (~2.5s); completed history is not
// polled while the active tab is shown.
usePolling(() => refreshCurrent(false), 2500);
</script>

<template>
  <section class="stack barmaker-view">
    <BarmakerPageHeader
      eyebrow="ESPACE BARMAKER"
      title="Tableau des commandes"
      :description="`${store.activeSummaries.length} commande(s) active(s).`" />

    <section class="filter-bar" aria-label="Filtrer les commandes">
      <button
        type="button"
        :class="{ active: tab === 'active' }"
        :aria-pressed="tab === 'active'"
        @click="selectTab('active')">
        Actives
      </button>
      <button
        type="button"
        :class="{ active: tab === 'completed' }"
        :aria-pressed="tab === 'completed'"
        @click="selectTab('completed')">
        Terminées
      </button>
    </section>

    <p
      v-if="store.listError && orders.length"
      class="alert warning"
      role="status">
      {{ store.listError }}
    </p>

    <section
      v-if="loading && !orders.length"
      class="card empty-state"
      aria-busy="true">
      <h2>Chargement des commandes…</h2>
    </section>

    <section
      v-else-if="store.listError && !orders.length"
      class="card empty-state">
      <h2>Impossible de charger les commandes</h2>
      <p>{{ store.listError }}</p>
      <button class="button" type="button" @click="refreshCurrent(true)">
        Réessayer
      </button>
    </section>

    <div v-else-if="orders.length" class="card order-table">
      <OrderSummaryCard
        v-for="order in orders"
        :key="order.id"
        :order="order" />
    </div>

    <section v-else class="card empty-state">
      <h2>
        {{
          tab === "active"
            ? "Aucune commande à traiter"
            : "Aucune commande terminée"
        }}
      </h2>
      <p>
        {{
          tab === "active"
            ? "Les nouvelles commandes apparaîtront ici dès leur validation."
            : "Les commandes terminées s’afficheront ici."
        }}
      </p>
    </section>
  </section>
</template>

<style scoped>
.barmaker-view {
  gap: 30px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: var(--space-4);
  border: 1px solid rgba(229, 219, 204, 0.84);
  border-radius: 24px;
  background: var(--color-panel);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.64);
}
.filter-bar button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 18px;
  border: 1px solid rgba(229, 219, 204, 0.92);
  border-radius: 14px;
  background: #fff;
  color: var(--color-primary);
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(29, 43, 31, 0.04);
}
.filter-bar button.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  box-shadow: 0 8px 18px rgba(29, 43, 31, 0.12);
}
.order-table {
  padding: 0;
  overflow: hidden;
  border-radius: 22px;
  box-shadow: var(--shadow-card);
}
@media (max-width: 720px) {
  .filter-bar {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: var(--space-2);
  }
  .filter-bar button {
    flex: 0 0 auto;
  }
}
</style>
