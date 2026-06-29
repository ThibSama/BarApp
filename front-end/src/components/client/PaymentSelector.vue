<script setup lang="ts">
import type { PaymentMethod } from '@/types/domain';
import { paymentGroupLabels, paymentMethodOptions } from '@/utils/payments';

const selectedPaymentMethod = defineModel<PaymentMethod | ''>({ required: true });
const groups = ['counter', 'application'] as const;
</script>

<template>
  <section class="card payment-section" aria-labelledby="payment-title">
    <h2 id="payment-title">Choisir le mode de paiement</h2>
    <div class="payment-groups">
      <fieldset v-for="group in groups" :key="group" class="payment-group">
        <legend>{{ paymentGroupLabels[group] }}</legend>
        <label
          v-for="method in paymentMethodOptions.filter((option) => option.group === group)"
          :key="method.id"
          class="payment-card"
          :class="{ selected: selectedPaymentMethod === method.id }"
        >
          <input v-model="selectedPaymentMethod" type="radio" name="payment-method" :value="method.id" :aria-label="method.label" />
          <span class="payment-icon" aria-hidden="true">{{ method.icon }}</span>
          <span>{{ method.label }}</span>
        </label>
      </fieldset>
    </div>
  </section>
</template>

<style scoped>
.payment-section { display: grid; gap: 1rem; }
.payment-section h2 { margin-bottom: 0; }
.payment-groups { display: grid; gap: 1rem; }
.payment-group { display: grid; gap: 0.75rem; margin: 0; }
.payment-card {
  display: grid;
  grid-template-columns: auto 3rem 1fr;
  align-items: center;
  gap: 0.75rem;
  min-height: 4rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.85rem;
  padding: 0.8rem;
  background: #fff;
  cursor: pointer;
  transition: border-color 120ms ease, box-shadow 120ms ease, background 120ms ease;
}
.payment-card:hover { border-color: #93c5fd; background: #f8fbff; }
.payment-card.selected { border-color: #2563eb; box-shadow: 0 0 0 3px #dbeafe; background: #eff6ff; }
.payment-card input { width: 1.15rem; height: 1.15rem; }
.payment-icon { display: inline-grid; place-items: center; min-width: 2.5rem; min-height: 2.5rem; border-radius: 0.75rem; background: #f1f5f9; color: #0f172a; font-weight: 900; font-size: 0.95rem; }
@media (min-width: 720px) { .payment-groups { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
