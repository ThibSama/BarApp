<script setup lang="ts">
defineProps<{ label: string; tone?: 'neutral' | 'success' | 'warning' | 'danger' }>();
</script>

<template>
  <span class="badge" :class="tone ?? 'neutral'">{{ label }}</span>
</template>

<style scoped>
.badge { display: inline-flex; align-items: center; border-radius: 999px; padding: 0.25rem 0.65rem; font-size: 0.82rem; font-weight: 700; }
.neutral { background: #eef2f7; color: #334155; }
.success { background: #dcfce7; color: #166534; }
.warning { background: #fef3c7; color: #92400e; }
.danger { background: #fee2e2; color: #991b1b; }
</style>
