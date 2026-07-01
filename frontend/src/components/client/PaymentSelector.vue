<script setup lang="ts">
import type { ApiPaymentMethod } from '@/types/api';
import { paymentGroupLabels, paymentMethodOptions } from '@/utils/payments';

const selectedPaymentMethod = defineModel<ApiPaymentMethod | ''>({ required: true });
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
.payment-section { display: grid; gap: 0.85rem; background: var(--color-panel); }
.payment-section h2 { margin-bottom: 0; }
.payment-groups { display: grid; gap: 0.85rem; }
.payment-group { display: grid; gap: 0.55rem; margin: 0; padding: 0.85rem; background: rgba(255,255,255,0.72); }
.payment-card {
  display: grid;
  grid-template-columns: auto 3rem 1fr;
  align-items: center;
  gap: 0.75rem;
  min-height: 3.25rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-medium);
  padding: 0.55rem 0.65rem;
  background: #fff;
  cursor: pointer;
  transition: border-color 120ms ease, box-shadow 120ms ease, background 120ms ease;
}
.payment-card:hover { border-color: rgba(29,43,31,0.35); background: #fbfaf7; }
.payment-card.selected { border-color: var(--color-primary); box-shadow: var(--shadow-focus); background: #f5f2ed; }
.payment-card input { width: 1.15rem; height: 1.15rem; }
.payment-icon { display: inline-grid; place-items: center; min-width: 2.2rem; min-height: 2.2rem; border-radius: var(--radius-small); background: var(--color-surface-muted); color: var(--color-primary); font-weight: 900; font-size: 0.85rem; }
@media (min-width: 720px) { .payment-groups { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
