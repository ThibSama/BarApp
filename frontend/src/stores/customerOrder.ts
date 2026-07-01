import { defineStore } from 'pinia';
import { ref } from 'vue';
import { ApiError } from '@/services/apiClient';
import { createOrder, fetchOrder } from '@/services/customerOrderApi';
import type { CreateOrderPayload, CustomerOrder } from '@/types/api';

function describeError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.isNetworkError) return 'Le serveur est injoignable. Vérifiez votre connexion.';
    switch (err.code) {
      case 'ORDER_NOT_FOUND':
        return 'Cette commande est introuvable.';
      case 'INVALID_IDENTIFIER':
        return 'Identifiant de commande invalide.';
      case 'COCKTAIL_NOT_FOUND':
      case 'COCKTAIL_UNAVAILABLE':
      case 'SIZE_UNAVAILABLE':
      case 'PRICE_UNAVAILABLE':
        return 'Un cocktail de votre panier n’est plus disponible.';
      default:
        return err.message || 'Une erreur est survenue. Réessayez.';
    }
  }
  return 'Une erreur est survenue. Réessayez.';
}

function isAbort(err: unknown): boolean {
  return err instanceof DOMException && err.name === 'AbortError';
}

function isNotFound(err: unknown): boolean {
  return (
    err instanceof ApiError && (err.code === 'ORDER_NOT_FOUND' || err.code === 'INVALID_IDENTIFIER')
  );
}

/**
 * Customer-facing order store shared by checkout, confirmation and tracking. It
 * owns the single source of truth for the currently displayed order, with
 * stale-response protection so a slow poll can never overwrite fresher data, and
 * non-destructive background refresh (transient failures keep the visible order).
 */
export const useCustomerOrderStore = defineStore('customerOrder', () => {
  const order = ref<CustomerOrder | null>(null);
  const loading = ref(false);
  const error = ref('');
  const notFound = ref(false);
  const submitting = ref(false);
  const submitError = ref('');
  const lastOrderId = ref('');

  // Monotonic sequence guarding against out-of-order GET completions.
  let loadSeq = 0;

  /**
   * Submit the cart as a real order. Resolves with the created order on success
   * (the caller clears the cart and navigates), throws the {@link ApiError}
   * otherwise so the view can map field errors and keep the cart intact.
   */
  async function submit(payload: CreateOrderPayload): Promise<CustomerOrder> {
    if (submitting.value) throw new ApiError({ message: 'Envoi déjà en cours.', status: 409 });
    submitting.value = true;
    submitError.value = '';
    try {
      const created = await createOrder(payload);
      lastOrderId.value = created.id;
      // Seed the store so confirmation can render instantly while still being
      // reload-safe (it re-fetches by id).
      order.value = created;
      loadSeq++;
      return created;
    } catch (err) {
      submitError.value = describeError(err);
      throw err;
    } finally {
      submitting.value = false;
    }
  }

  async function loadOrder(orderId: string, options: { initial?: boolean } = {}): Promise<void> {
    const seq = ++loadSeq;
    if (options.initial) {
      loading.value = true;
      error.value = '';
      notFound.value = false;
    }
    try {
      const data = await fetchOrder(orderId);
      if (seq !== loadSeq) return; // superseded by a newer load
      order.value = data;
      error.value = '';
      notFound.value = false;
    } catch (err) {
      if (isAbort(err) || seq !== loadSeq) return;
      const missing = isNotFound(err);
      // Background poll failures keep the visible order; only the initial load
      // (or a hard not-found) replaces the screen.
      if (options.initial || missing) {
        notFound.value = missing;
        error.value = describeError(err);
        if (options.initial && missing) order.value = null;
      }
    } finally {
      if (seq === loadSeq) loading.value = false;
    }
  }

  function reset(): void {
    order.value = null;
    loading.value = false;
    error.value = '';
    notFound.value = false;
    submitError.value = '';
  }

  return {
    order,
    loading,
    error,
    notFound,
    submitting,
    submitError,
    lastOrderId,
    submit,
    loadOrder,
    reset,
  };
});
