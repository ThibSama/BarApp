<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import PaymentSelector from '@/components/client/PaymentSelector.vue';
import QuantitySelector from '@/components/common/QuantitySelector.vue';
import { useCartStore } from '@/stores/cart';
import { useOrderStore } from '@/stores/orders';
import type { PaymentMethod } from '@/types/domain';
import { formatCurrency } from '@/utils/formatters';
import { getPaymentSimulationMessage } from '@/utils/payments';

const cart = useCartStore();
const orders = useOrderStore();
const router = useRouter();
const shippingFee = 0;
const selectedPaymentMethod = ref<PaymentMethod | ''>('');
const paymentError = ref('');

function confirmOrder(): void {
  if (!selectedPaymentMethod.value) {
    paymentError.value = 'Sélectionnez un mode de paiement pour valider la commande.';
    return;
  }
  const order = orders.createOrderFromCart(selectedPaymentMethod.value);
  if (order) router.push(`/client/confirmation/${order.id}`);
}
</script>

<template>
  <section class="stack">
    <div class="page-title"><div><p class="eyebrow">Espace client</p><h1>Panier</h1></div><RouterLink class="button secondary" to="/client/menu">Continuer la commande</RouterLink></div>
    <div v-if="cart.detailedItems.length" class="cart-layout">
      <div class="stack">
        <article v-for="item in cart.detailedItems" :key="item.id" class="card cart-item">
          <div><h2>{{ item.cocktail?.name ?? 'Cocktail retiré' }}</h2><p>Taille {{ item.size }} · {{ formatCurrency(item.unitPrice) }} l’unité</p></div>
          <QuantitySelector :model-value="item.quantity" label="Quantité du cocktail" @update:model-value="cart.updateQuantity(item.id, $event)" />
          <strong>{{ formatCurrency(item.lineTotal) }}</strong>
          <button class="button ghost" type="button" @click="cart.removeItem(item.id)">Retirer</button>
        </article>
        <PaymentSelector v-model="selectedPaymentMethod" />
        <p v-if="paymentError" class="alert error" role="alert">{{ paymentError }}</p>
        <p v-if="selectedPaymentMethod" class="alert success">{{ getPaymentSimulationMessage(selectedPaymentMethod) }}</p>
      </div>
      <aside class="card summary"><h2>Résumé</h2><p><span>Sous-total</span><strong>{{ formatCurrency(cart.total) }}</strong></p><p><span>Frais</span><strong>{{ formatCurrency(shippingFee) }}</strong></p><p class="total"><span>Total</span><strong>{{ formatCurrency(cart.total + shippingFee) }}</strong></p><p v-if="!selectedPaymentMethod" class="muted">Sélectionnez un mode de paiement pour valider la commande.</p><button class="button full" type="button" :disabled="!selectedPaymentMethod" @click="confirmOrder">Confirmer la commande</button></aside>
    </div>
    <section v-else class="card empty-state"><h2>Votre panier est vide</h2><p>Ajoutez un cocktail depuis la carte pour commencer.</p><RouterLink class="button" to="/client/menu">Voir la carte</RouterLink></section>
  </section>
</template>

<style scoped>
.cart-layout { display: grid; gap: 1.5rem; }
.cart-item { display: grid; gap: 1rem; align-items: center; }
.summary { display: grid; gap: 1rem; align-content: start; }
.summary p { display: flex; justify-content: space-between; gap: 1rem; }
.summary .muted { display: block; }
.total { font-size: 1.25rem; border-top: 1px solid #e2e8f0; padding-top: 1rem; }
.full { width: 100%; }
@media (min-width: 900px) { .cart-layout { grid-template-columns: 1fr 22rem; } .cart-item { grid-template-columns: 1fr auto auto auto; } }
</style>
