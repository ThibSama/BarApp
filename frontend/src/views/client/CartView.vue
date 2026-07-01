<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import PaymentSelector from '@/components/client/PaymentSelector.vue';
import CocktailImage from '@/components/common/CocktailImage.vue';
import QuantitySelector from '@/components/common/QuantitySelector.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { useCartStore } from '@/stores/cart';
import { useCustomerOrderStore } from '@/stores/customerOrder';
import type { ApiPaymentMethod } from '@/types/api';
import { formatCurrency } from '@/utils/formatters';
import { getPaymentSimulationMessage } from '@/utils/payments';
import { validateTableNumber } from '@/utils/validation';

const TABLE_KEY = 'barapp.checkout.tableNumber';

const cart = useCartStore();
const orders = useCustomerOrderStore();
const router = useRouter();

// Table number is persisted locally only while checkout is incomplete; it is
// cleared on a successful order.
const tableInput = ref<string>(localStorage.getItem(TABLE_KEY) ?? '');
const selectedPaymentMethod = ref<ApiPaymentMethod | ''>('');
const submitError = ref('');

watch(tableInput, (value) => {
  const text = String(value ?? '').trim();
  if (text) localStorage.setItem(TABLE_KEY, text);
  else localStorage.removeItem(TABLE_KEY);
});

const tableNumber = computed<number | null>(() => {
  const raw = String(tableInput.value ?? '').trim();
  if (raw === '') return null;
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : NaN;
});
const tableError = computed(() => validateTableNumber(tableNumber.value));
const tableValid = computed(() => tableError.value === '');
const paymentAvailable = computed(() => !cart.isEmpty && tableValid.value);
const canSubmit = computed(
  () => !cart.isEmpty && tableValid.value && Boolean(selectedPaymentMethod.value) && !orders.submitting,
);

// Show the table error only once the customer has typed something.
const showTableError = computed(() => String(tableInput.value ?? '').trim() !== '' && !tableValid.value);

async function confirmOrder(): Promise<void> {
  submitError.value = '';
  if (!canSubmit.value || !selectedPaymentMethod.value || tableNumber.value === null) return;
  try {
    const order = await orders.submit({
      items: cart.toOrderItems(),
      tableNumber: tableNumber.value,
      paymentMethod: selectedPaymentMethod.value,
    });
    // Success: clear cart, table number and payment, then navigate.
    cart.clearCart();
    tableInput.value = '';
    localStorage.removeItem(TABLE_KEY);
    selectedPaymentMethod.value = '';
    await router.push({ name: 'client-order-confirmation', params: { orderId: order.id } });
  } catch {
    // Failure: keep the cart, table number and payment; surface the message.
    submitError.value = orders.submitError;
  }
}
</script>

<template>
  <section class="stack">
    <div class="page-title cart-title"><div><h1>Votre panier</h1><div class="title-underline" aria-hidden="true"></div></div><RouterLink class="basket-button" :to="{ name: 'client-cart' }"><span aria-hidden="true"><AppIcon name="clipboard-list" :size="20" /></span> Panier <span class="basket-count">{{ cart.itemCount }}</span></RouterLink></div>
    <div v-if="!cart.isEmpty" class="cart-layout">
      <div class="cart-main stack">
        <div class="cart-list card">
          <article v-for="item in cart.items" :key="item.id" class="cart-item">
            <div class="cart-thumb-wrap">
              <CocktailImage class="cart-thumb" :image-url="item.imageUrlSnapshot ?? undefined" :cocktail-name="item.nameSnapshot" />
            </div>
            <div class="item-copy"><h2>{{ item.nameSnapshot }}</h2><p>Taille {{ item.size }} · {{ formatCurrency(item.unitPriceSnapshot) }} l’unité</p></div>
            <QuantitySelector :model-value="item.quantity" :label="`Quantité de ${item.nameSnapshot}`" @update:model-value="cart.updateQuantity(item.id, $event)" />
            <strong class="line-total">{{ formatCurrency(item.unitPriceSnapshot * item.quantity) }}</strong>
            <button class="remove-button" type="button" :aria-label="`Retirer ${item.nameSnapshot} du panier`" @click="cart.removeItem(item.id)"><AppIcon name="trash" :size="18" /></button>
          </article>
        </div>

        <section class="card table-card" aria-labelledby="table-title">
          <h2 id="table-title">Numéro de table</h2>
          <p class="muted">Indiquez votre table (1 à 25) pour activer le paiement.</p>
          <label class="table-field">
            <span class="visually-hidden">Numéro de table</span>
            <input v-model="tableInput" type="number" inputmode="numeric" min="1" max="25" step="1" placeholder="Ex. 12" aria-describedby="table-help" />
          </label>
          <p v-if="showTableError" id="table-help" class="alert error" role="alert">{{ tableError }}</p>
        </section>

        <PaymentSelector v-if="paymentAvailable" v-model="selectedPaymentMethod" class="payment-compact" />
        <p v-else class="alert warning" role="status">Veuillez saisir votre numéro de table</p>

        <p v-if="submitError" class="alert error" role="alert">{{ submitError }}</p>
        <p v-else-if="selectedPaymentMethod" class="alert success">{{ getPaymentSimulationMessage(selectedPaymentMethod) }}</p>
      </div>
      <aside class="card summary"><h2>Récapitulatif</h2><p><span>Sous-total</span><strong>{{ formatCurrency(cart.total) }}</strong></p><p class="total"><span>Total</span><strong>{{ formatCurrency(cart.total) }}</strong></p><p v-if="!canSubmit" class="muted">Renseignez votre table puis un mode de paiement pour valider la commande.</p><RouterLink class="summary-link" :to="{ name: 'client-menu' }">Continuer la commande</RouterLink><button class="button full" type="button" :disabled="!canSubmit" @click="confirmOrder">{{ orders.submitting ? 'Envoi en cours…' : 'Valider la commande' }}</button></aside>
    </div>
    <section v-else class="card empty-state"><h2>Votre panier est vide</h2><p>Ajoutez des cocktails pour commencer votre commande.</p><RouterLink class="button" :to="{ name: 'client-menu' }">Parcourir la carte</RouterLink></section>
  </section>
</template>

<style scoped>
.cart-title { align-items: center; margin-bottom: 2px; }
.basket-button { align-self: center; display: inline-flex; align-items: center; gap: 9px; min-height: 46px; padding: 0 16px; border-radius: 14px; background: var(--color-primary); color: #fff; font-weight: 800; }
.basket-button:hover { text-decoration: none; background: var(--color-primary-hover); }
.basket-count { display: inline-grid; place-items: center; min-width: 24px; height: 24px; padding: 0 7px; border-radius: var(--radius-round); background: #fff; color: var(--color-primary); font-size: 0.78rem; }
.cart-layout { display: grid; gap: 28px; }
.cart-main { min-width: 0; }
.cart-list { display: grid; gap: 0; padding: 0; overflow: hidden; box-shadow: var(--shadow-card-soft); }
.cart-item { display: grid; gap: 16px; align-items: center; padding: 16px 18px; border-bottom: 1px solid var(--color-border); }
.cart-item:last-child { border-bottom: 0; }
.cart-thumb-wrap { width: 72px; height: 72px; border-radius: 16px; overflow: hidden; background: #f4efe6; display: grid; place-items: center; }
.cart-thumb { width: 100%; height: 100%; object-fit: cover; }
.item-copy { min-width: 0; }
.item-copy h2 { margin-bottom: 5px; overflow-wrap: anywhere; font-size: 1.08rem; }
.item-copy p { margin: 0; color: var(--color-text-secondary); font-size: 0.9rem; }
.line-total { min-width: 72px; text-align: right; font-size: 1rem; }
.remove-button { width: 38px; height: 38px; border: 0; border-radius: var(--radius-round); background: #f8f6f2; color: var(--color-text-secondary); cursor: pointer; display: grid; place-items: center; }
.remove-button:hover { background: #fde8e7; color: var(--color-error); }
.table-card { display: grid; gap: var(--space-2); box-shadow: var(--shadow-card-soft); }
.table-card h2 { margin: 0; }
.table-card .muted { margin: 0; }
.table-field input { min-height: 54px; max-width: 12rem; border-radius: 14px; }
.summary { display: grid; gap: 15px; align-content: start; padding: 24px; box-shadow: var(--shadow-card); }
.summary h2 { margin-bottom: 3px; }
.summary p { display: flex; justify-content: space-between; gap: var(--space-4); }
.summary .muted { display: block; }
.total { font-size: 1.22rem; border-top: 1px solid var(--color-border); padding-top: var(--space-4); }
.full { width: 100%; }
.summary-link { justify-self: start; color: var(--color-text-secondary); font-size: 0.92rem; }
.payment-compact { margin-top: 4px; }
@media (min-width: 900px) { .cart-layout { grid-template-columns: minmax(0, 1fr) minmax(280px, 31%); align-items: start; } .cart-item { grid-template-columns: 72px minmax(0, 1fr) auto auto 38px; } .summary { position: sticky; top: 32px; } }
@media (max-width: 640px) { .cart-item { grid-template-columns: 72px minmax(0, 1fr); } .cart-item > .quantity-control, .cart-item > .line-total, .cart-item > .remove-button { grid-column: 2; justify-self: start; } }
</style>
