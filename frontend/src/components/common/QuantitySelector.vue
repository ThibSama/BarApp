<script setup lang="ts">
const model = defineModel<number>({ required: true });
defineProps<{ min?: number; label: string }>();
</script>

<template>
  <div class="quantity-control" role="group" :aria-label="label">
    <button type="button" :aria-label="`${label} : diminuer la quantité`" @click="model = Math.max(min ?? 1, model - 1)" :disabled="model <= (min ?? 1)">−</button>
    <output :aria-label="`${label} : quantité ${model}`">{{ model }}</output>
    <button type="button" :aria-label="`${label} : augmenter la quantité`" @click="model += 1">+</button>
  </div>
</template>

<style scoped>
.quantity-control { display: inline-flex; align-items: center; gap: 6px; border: 0; border-radius: var(--radius-round); overflow: visible; background: transparent; min-height: 38px; }
button { border: 1px solid var(--color-border); border-radius: var(--radius-round); background: #f7f4ee; min-width: 36px; min-height: 36px; font-weight: 900; cursor: pointer; color: var(--color-primary); }
button:disabled { cursor: not-allowed; color: #b8b8b8; }
output { min-width: 24px; text-align: center; font-weight: 800; }
</style>
