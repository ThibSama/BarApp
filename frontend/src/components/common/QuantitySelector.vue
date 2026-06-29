<script setup lang="ts">
const model = defineModel<number>({ required: true });
defineProps<{ min?: number; label: string }>();
</script>

<template>
  <div class="quantity-control" :aria-label="label">
    <button type="button" @click="model = Math.max(min ?? 1, model - 1)" :disabled="model <= (min ?? 1)">−</button>
    <output>{{ model }}</output>
    <button type="button" @click="model += 1">+</button>
  </div>
</template>

<style scoped>
.quantity-control { display: inline-flex; align-items: center; border: 1px solid #cbd5e1; border-radius: 0.75rem; overflow: hidden; background: #fff; }
button { border: 0; background: #f8fafc; min-width: 2.5rem; min-height: 2.5rem; font-weight: 800; cursor: pointer; }
button:disabled { cursor: not-allowed; color: #94a3b8; }
output { min-width: 2.5rem; text-align: center; font-weight: 700; }
</style>
