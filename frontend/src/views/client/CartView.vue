<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import PaymentSelector from '@/components/client/PaymentSelector.vue';
import CocktailImage from '@/components/common/CocktailImage.vue';
import QuantitySelector from '@/components/common/QuantitySelector.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { useCatalogStore } from '@/stores/catalog';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/orders';
import type { PaymentMethod } from '@/types/domain';
import { formatCurrency } from '@/utils/formatters';
import { getPaymentSimulationMessage } from '@/utils/payments';

const cart = useCartStore();
const catalog = useCatalogStore();
const orders = useOrderStore();
const router = useRouter();
const shippingFee = 0;
const selectedPaymentMethod = ref<PaymentMethod | ''>('');
const paymentError = ref('');
const isSubmitting = ref(false);

function categoryName(categoryId?: string): string {
  if (!categoryId) return 'Cocktail du bar';
  const category = catalog.getCategoryById(categoryId);
  return category?.name ?? 'Cocktail du bar';
}

async function confirmOrder(): Promise<void> {
  if (isSubmitting.value) return;
  if (!selectedPaymentMethod.value) {
    paymentError.value = 'Sélectionnez un mode de paiement pour valider la commande.';
    return;
  }
  isSubmitting.value = true;
  paymentError.value = '';
  await Promise.resolve();
  const order = orders.createOrderFromCart(selectedPaymentMethod.value);
  if (order) router.push(`/client/confirmation/${order.id}`);
  else {
    paymentError.value = 'Impossible de valider une commande vide.';
    isSubmitting.value = false;
  }
}
</script>

<template>
  <section class="stack">
    <div class="page-title cart-title"><div><h1>Votre panier</h1><div class="title-underline" aria-hidden="true"></div></div><RouterLink class="basket-button" to="/client/panier"><span aria-hidden="true"><AppIcon name="clipboard-list" :size="20" /></span> Panier <span class="basket-count">{{ cart.itemCount }}</span></RouterLink></div>
    <div v-if="cart.detailedItems.length" class="cart-layout">
      <div class="cart-main stack">
        <div class="cart-list card">
          <article v-for="item in cart.detailedItems" :key="item.id" class="cart-item">
            <div class="cart-thumb-wrap">
              <CocktailImage class="cart-thumb" :image-url="item.cocktail?.imageUrl" :cocktail-name="item.cocktail?.name" />
            </div>
            <div class="item-copy"><h2>{{ item.cocktail?.name ?? 'Cocktail retiré' }}</h2><p>{{ categoryName(item.cocktail?.categoryId) }} · Taille {{ item.size }} · {{ formatCurrency(item.unitPrice) }} l’unité</p></div>
            <QuantitySelector :model-value="item.quantity" :label="`Quantité de ${item.cocktail?.name ?? 'ce cocktail'}`" @update:model-value="cart.updateQuantity(item.id, $event)" />
            <strong class="line-total">{{ formatCurrency(item.lineTotal) }}</strong>
            <button class="remove-button" type="button" :aria-label="`Retirer ${item.cocktail?.name ?? 'ce cocktail'} du panier`" @click="cart.removeItem(item.id)"><AppIcon name="trash" :size="18" /></button>
          </article>
        </div>
        <PaymentSelector v-model="selectedPaymentMethod" class="payment-compact" />
        <p v-if="paymentError" class="alert error" role="alert">{{ paymentError }}</p>
        <p v-if="selectedPaymentMethod" class="alert success">{{ getPaymentSimulationMessage(selectedPaymentMethod) }}</p>
      </div>
      <aside class="card summary"><h2>Récapitulatif</h2><p><span>Sous-total</span><strong>{{ formatCurrency(cart.total) }}</strong></p><p v-if="shippingFee"><span>Frais</span><strong>{{ formatCurrency(shippingFee) }}</strong></p><p class="total"><span>Total</span><strong>{{ formatCurrency(cart.total + shippingFee) }}</strong></p><p v-if="!selectedPaymentMethod" class="muted">Sélectionnez un mode de paiement ci-dessous pour valider la commande.</p><RouterLink class="summary-link" to="/client/menu">Continuer la commande</RouterLink><button class="button full" type="button" :disabled="!selectedPaymentMethod || isSubmitting" @click="confirmOrder">{{ isSubmitting ? 'Envoi en cours…' : 'Valider la commande' }}</button></aside>
    </div>
    <section v-else class="card empty-state"><h2>Votre panier est vide</h2><p>Ajoutez des cocktails pour commencer votre commande.</p><RouterLink class="button" to="/client/menu">Parcourir la carte</RouterLink></section>
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
