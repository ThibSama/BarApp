import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { ApiError } from '@/services/apiClient';
import {
  advanceOrderItem,
  fetchOrderDetail,
  fetchOrderSummaries,
} from '@/services/barmakerOrderApi';
import type { BarOrderDetail, BarOrderSummary } from '@/types/api';

/** Map an error to a controlled French message (never a raw backend trace). */
function describeError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.isNetworkError) return 'Le serveur est injoignable. Réessayez.';
    switch (err.code) {
      case 'ORDER_NOT_FOUND':
        return 'Cette commande est introuvable.';
      case 'ORDER_ITEM_NOT_FOUND':
        return 'Ce cocktail n’existe plus dans la commande.';
      case 'INVALID_PREPARATION_TRANSITION':
        return 'Ce cocktail a déjà changé d’état ou est déjà terminé.';
      case 'INVALID_IDENTIFIER':
        return 'Identifiant de commande invalide.';
      default:
        if (err.status === 401) return 'Session expirée. Reconnectez-vous.';
        return 'Une erreur est survenue. Réessayez.';
    }
  }
  return 'Une erreur est survenue. Réessayez.';
}

function isAbort(err: unknown): boolean {
  return err instanceof DOMException && err.name === 'AbortError';
}

export const useBarmakerOrderStore = defineStore('barmakerOrders', () => {
  const activeSummaries = ref<BarOrderSummary[]>([]);
  const completedSummaries = ref<BarOrderSummary[]>([]);
  const detail = ref<BarOrderDetail | null>(null);

  const loadingActive = ref(false);
  const loadingCompleted = ref(false);
  const loadingDetail = ref(false);
  // Per-item pending set so only the item being advanced is disabled.
  const pendingItemIds = ref<string[]>([]);

  const listError = ref('');
  const detailError = ref('');
  // Distinguishes a hard not-found from a recoverable error in the detail view.
  const detailNotFound = ref(false);
  const lastRefreshedAt = ref<number | null>(null);

  // Overlap guards: a poll tick is skipped while a request of the same kind is
  // already in flight, so navigation never stacks duplicate requests.
  let activeInFlight = false;
  let completedInFlight = false;
  // Monotonic sequence so a slow GET cannot overwrite a fresher detail state.
  let detailSeq = 0;

  const hasActiveLoaded = computed(() => lastRefreshedAt.value !== null);

  function isItemPending(id: string): boolean {
    return pendingItemIds.value.includes(id);
  }

  async function loadActive(options: { initial?: boolean } = {}): Promise<void> {
    if (activeInFlight) return;
    activeInFlight = true;
    if (options.initial) loadingActive.value = true;
    try {
      activeSummaries.value = await fetchOrderSummaries(false);
      listError.value = '';
      lastRefreshedAt.value = Date.now();
    } catch (err) {
      if (isAbort(err)) return;
      // Non-destructive: keep the last good data and surface a soft warning.
      listError.value = describeError(err);
    } finally {
      activeInFlight = false;
      loadingActive.value = false;
    }
  }

  async function loadCompleted(options: { initial?: boolean } = {}): Promise<void> {
    if (completedInFlight) return;
    completedInFlight = true;
    if (options.initial) loadingCompleted.value = true;
    try {
      completedSummaries.value = await fetchOrderSummaries(true);
      listError.value = '';
    } catch (err) {
      if (isAbort(err)) return;
      listError.value = describeError(err);
    } finally {
      completedInFlight = false;
      loadingCompleted.value = false;
    }
  }

  async function loadDetail(orderId: string, options: { initial?: boolean } = {}): Promise<void> {
    const seq = ++detailSeq;
    if (options.initial) {
      loadingDetail.value = true;
      detailError.value = '';
      detailNotFound.value = false;
    }
    try {
      const data = await fetchOrderDetail(orderId);
      if (seq !== detailSeq) return; // a newer request superseded this one
      detail.value = data;
      detailError.value = '';
      detailNotFound.value = false;
    } catch (err) {
      if (isAbort(err) || seq !== detailSeq) return;
      const isNotFound = err instanceof ApiError && err.code === 'ORDER_NOT_FOUND';
      // Background poll failures keep the visible order; only hard initial loads
      // (or not-found) replace the screen.
      if (options.initial || isNotFound) {
        detailNotFound.value = isNotFound;
        detailError.value = describeError(err);
        if (options.initial && isNotFound) detail.value = null;
      }
    } finally {
      if (seq === detailSeq) loadingDetail.value = false;
    }
  }

  /**
   * Advance one cocktail by exactly one step. Replaces the detail with the
   * backend's refreshed order and refreshes the active queue. Returns an error
   * message for the view to render against that item, or null on success.
   */
  async function advance(itemId: string): Promise<string | null> {
    if (isItemPending(itemId)) return null;
    pendingItemIds.value = [...pendingItemIds.value, itemId];
    try {
      const updated = await advanceOrderItem(itemId);
      // Bump the sequence so any in-flight GET cannot overwrite this fresher
      // mutation response with stale data.
      detailSeq++;
      detail.value = updated;
      detailError.value = '';
      detailNotFound.value = false;
      void loadActive();
      return null;
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        // Session already invalidated by the HTTP client; let the view redirect.
        return describeError(err);
      }
      const message = describeError(err);
      // For state-conflict / missing-item errors, refresh the detail so the UI
      // reflects the real backend state.
      if (
        err instanceof ApiError &&
        (err.code === 'INVALID_PREPARATION_TRANSITION' || err.code === 'ORDER_ITEM_NOT_FOUND') &&
        detail.value
      ) {
        void loadDetail(detail.value.id, { initial: false });
      }
      return message;
    } finally {
      pendingItemIds.value = pendingItemIds.value.filter((id) => id !== itemId);
    }
  }

  function reset(): void {
    detail.value = null;
    detailError.value = '';
    detailNotFound.value = false;
    loadingDetail.value = false;
  }

  return {
    activeSummaries,
    completedSummaries,
    detail,
    loadingActive,
    loadingCompleted,
    loadingDetail,
    pendingItemIds,
    listError,
    detailError,
    detailNotFound,
    lastRefreshedAt,
    hasActiveLoaded,
    isItemPending,
    loadActive,
    loadCompleted,
    loadDetail,
    advance,
    reset,
  };
});
