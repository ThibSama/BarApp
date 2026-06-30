<script setup lang="ts">
defineProps<{ label: string; tone?: 'neutral' | 'success' | 'warning' | 'danger' }>();
</script>

<template>
  <span class="badge" :class="tone ?? 'neutral'">{{ label }}</span>
</template>

<style scoped>
.badge { display: inline-flex; align-items: center; justify-content: center; gap: 0.42rem; width: fit-content; min-height: 28px; border-radius: var(--radius-round); padding: 0.28rem 0.72rem; border: 1px solid transparent; font-size: 0.78rem; line-height: 1; font-weight: 850; white-space: nowrap; }
.badge::before { content: ''; width: 0.42rem; height: 0.42rem; border-radius: 50%; background: currentColor; opacity: 0.85; }
.neutral { background: #eef2f7; border-color: #dbe3ee; color: #334155; }
.success { background: #e8f3e9; border-color: #cfe7d1; color: var(--color-success); }
.warning { background: #fff6dd; border-color: #f6df9d; color: #8a5a00; }
.danger { background: #fde8e7; border-color: #f4c4c0; color: #9d1f18; }
</style>
