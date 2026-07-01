<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BarmakerPageHeader from '@/components/barmaker/BarmakerPageHeader.vue';
import BarPreparationProgress from '@/components/barmaker/BarPreparationProgress.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useBarmakerOrderStore } from '@/stores/barmakerOrders';
import { useAuthStore } from '@/stores/auth';
import { usePolling } from '@/composables/usePolling';
import {
  apiOrderStatusLabels,
  apiOrderStatusTone,
  apiPreparationStatusLabels,
  formatCurrency,
  formatTime,
} from '@/utils/formatters';
import { getPaymentMethodLabel } from '@/utils/payments';
import type { ApiPreparationStatus, BarOrderItem } from '@/types/api';

const route = useRoute();
const router = useRouter();
const store = useBarmakerOrderStore();
const auth = useAuthStore();

const orderId = computed(() => String(route.params.orderId));
const order = computed(() => store.detail);
const itemErrors = ref<Record<string, string>>({});

const NEXT_LABEL: Record<ApiPreparationStatus, string> = {
  PREPARATION_INGREDIENTS: 'Commencer l’assemblage',
  ASSEMBLY: 'Passer au dressage',
  DRESSING: 'Terminer le cocktail',
  COMPLETED: 'Cocktail terminé',
};

function isCompleted(item: BarOrderItem): boolean {
  return item.preparationStatus === 'COMPLETED';
}

function redirectIfLoggedOut(): boolean {
  if (!auth.accessToken) {
    void router.replace({ name: 'bar-login', query: { redirect: route.fullPath } });
    return true;
  }
  return false;
}

async function advance(item: BarOrderItem): Promise<void> {
  if (isCompleted(item) || store.isItemPending(item.id)) return;
  itemErrors.value = { ...itemErrors.value, [item.id]: '' };
  const error = await store.advance(item.id);
  if (redirectIfLoggedOut()) return;
  if (error) itemErrors.value = { ...itemErrors.value, [item.id]: error };
}

onMounted(() => store.loadDetail(orderId.value, { initial: true }));

// Keep the detail fresh for a second device; stop hammering once completed.
usePolling(() => {
  if (order.value?.status === 'COMPLETED') return;
  void store.loadDetail(orderId.value, { initial: false }).then(() => {
    redirectIfLoggedOut();
  });
}, 2500);
</script>

<template>
  <section v-if="store.loadingDetail && !order" class="card empty-state" aria-busy="true">
    <h1>Chargement de la commande…</h1>
  </section>

  <section v-else-if="store.detailNotFound" class="card empty-state">
    <h1>Commande introuvable</h1>
    <RouterLink class="button" to="/bar/orders">Retour aux commandes</RouterLink>
  </section>

  <section v-else-if="!order && store.detailError" class="card empty-state">
    <h1>Impossible de charger la commande</h1>
    <p>{{ store.detailError }}</p>
    <button class="button" type="button" @click="store.loadDetail(orderId, { initial: true })">Réessayer</button>
  </section>

  <section v-else-if="order" class="stack order-detail-page">
    <RouterLink class="back-link" to="/bar/orders"><AppIcon name="arrow-left" :size="18" />Retour aux commandes</RouterLink>
    <div class="detail-header-row">
      <BarmakerPageHeader eyebrow="DÉTAIL COMMANDE" :title="order.publicCode" :description="`Table ${order.tableNumber} · créée le ${formatTime(order.createdAt)}`" />
      <StatusBadge :label="apiOrderStatusLabels[order.status]" :tone="apiOrderStatusTone(order.status)" />
    </div>

    <div class="order-detail-layout">
      <div class="stack prep-list">
        <article v-for="item in order.items" :key="item.id" class="prep-card">
          <div class="prep-copy">
            <p class="eyebrow">Cocktail #{{ item.sequenceNumber }}</p>
            <h2>{{ item.cocktailName }}</h2>
            <p class="muted">Taille {{ item.size }} · {{ formatCurrency(item.unitPrice) }} · {{ apiPreparationStatusLabels[item.preparationStatus] }}</p>
            <BarPreparationProgress :current-step="item.preparationStatus" />
            <p v-if="itemErrors[item.id]" class="alert error" role="alert">{{ itemErrors[item.id] }}</p>
          </div>
          <button
            class="button prep-action"
            type="button"
            :disabled="isCompleted(item) || store.isItemPending(item.id)"
            @click="advance(item)"
          >
            {{ store.isItemPending(item.id) ? 'Mise à jour…' : NEXT_LABEL[item.preparationStatus] }}
          </button>
        </article>
      </div>

      <aside class="order-summary">
        <h2>Résumé</h2>
        <p><span>Table</span><strong>{{ order.tableNumber }}</strong></p>
        <p><span>Statut</span><strong>{{ apiOrderStatusLabels[order.status] }}</strong></p>
        <p><span>Paiement</span><strong>{{ getPaymentMethodLabel(order.paymentMethod) }}</strong></p>
        <p><span>Cocktails</span><strong>{{ order.items.length }}</strong></p>
        <p class="total"><span>Total</span><strong>{{ formatCurrency(order.totalAmount) }}</strong></p>
        <p v-if="order.status === 'COMPLETED'" class="alert success">Tous les cocktails sont terminés.</p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.order-detail-page { gap: 30px; }
.back-link { width: fit-content; display: inline-flex; align-items: center; gap: var(--space-2); }
.detail-header-row { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--space-4); }
.order-detail-layout { display: grid; gap: var(--space-6); }
.prep-list { gap: var(--space-4); }
.prep-card { position: relative; display: grid; gap: var(--space-5); align-items: center; padding: 24px 24px 24px 28px; border: 1px solid rgba(229,219,204,0.9); border-radius: 22px; background: #fff; box-shadow: var(--shadow-card-soft); overflow: hidden; }
.prep-card::before { content: ''; position: absolute; left: 0; top: 20px; bottom: 20px; width: 3px; border-radius: var(--radius-round); background: var(--color-accent); opacity: 0.72; }
.prep-copy { display: grid; gap: var(--space-3); }
.prep-copy h2 { margin: 0; font-size: 1.24rem; letter-spacing: -0.025em; }
.prep-action { justify-self: start; min-height: 44px; border-radius: 14px; }
.order-summary { display: grid; gap: var(--space-4); align-content: start; padding: 24px; border: 1px solid rgba(229,219,204,0.9); border-radius: 22px; background: #fff; box-shadow: var(--shadow-card); }
.order-summary h2 { margin: 0; }
.order-summary p { display: flex; justify-content: space-between; gap: var(--space-4); margin: 0; }
.order-summary span { color: var(--color-text-secondary); }
.order-summary .alert { display: block; }
.total { border-top: 1px solid var(--color-border); padding-top: var(--space-4); }
@media (min-width: 920px) { .order-detail-layout { grid-template-columns: minmax(0, 1fr) 320px; align-items: start; } .prep-card { grid-template-columns: minmax(0, 1fr) auto; } .order-summary { position: sticky; top: 32px; } }
@media (max-width: 720px) { .detail-header-row { display: grid; } .prep-action { width: 100%; } }
</style>
