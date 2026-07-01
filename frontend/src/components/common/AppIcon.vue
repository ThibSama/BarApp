<script setup lang="ts">
type IconName = 'arrow-left' | 'check' | 'chevron-right' | 'clipboard-list' | 'clock' | 'grid' | 'menu' | 'martini' | 'pencil' | 'plus' | 'power' | 'search' | 'sliders' | 'tags' | 'trash' | 'users' | 'x';

withDefaults(defineProps<{ name: IconName; size?: number }>(), { size: 20 });
</script>

<template>
  <svg class="app-icon" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
    <template v-if="name === 'arrow-left'"><path d="m15 18-6-6 6-6" /><path d="M20 12H9" /></template>
    <template v-else-if="name === 'check'"><path d="m20 6-11 11-5-5" /></template>
    <template v-else-if="name === 'chevron-right'"><path d="m9 18 6-6-6-6" /></template>
    <template v-else-if="name === 'clipboard-list'"><rect x="8" y="2.5" width="8" height="4" rx="1.5" /><path d="M9 4H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2" /><path d="M9 11h6" /><path d="M9 15h6" /></template>
    <template v-else-if="name === 'clock'"><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></template>
    <template v-else-if="name === 'grid'"><path d="M4 5h16" /><path d="M4 12h16" /><path d="M4 19h16" /></template>
    <template v-else-if="name === 'menu'"><path d="M4 7h16" /><path d="M4 12h16" /><path d="M4 17h16" /></template>
    <template v-else-if="name === 'martini'"><path d="M7 3h10l-5 7-5-7Z" /><path d="M12 10v8" /><path d="M8.5 21h7" /><path d="M9 6h6" /></template>
    <template v-else-if="name === 'pencil'"><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5Z" /></template>
    <template v-else-if="name === 'plus'"><path d="M12 5v14" /><path d="M5 12h14" /></template>
    <template v-else-if="name === 'power'"><path d="M12 2v10" /><path d="M18.4 6.6a8 8 0 1 1-12.8 0" /></template>
    <template v-else-if="name === 'search'"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></template>
    <template v-else-if="name === 'sliders'"><path d="M4 7h9" /><path d="M17 7h3" /><path d="M14 5v4" /><path d="M4 17h3" /><path d="M11 17h9" /><path d="M8 15v4" /></template>
    <template v-else-if="name === 'tags'"><path d="M20.5 13.5 13 21l-9-9V4h8l8.5 8.5a1.4 1.4 0 0 1 0 2Z" /><path d="M7.5 7.5h.01" /></template>
    <template v-else-if="name === 'trash'"><path d="M3 6h18" /><path d="M8 6V4h8v2" /><path d="m6 6 1 15h10l1-15" /><path d="M10 11v6" /><path d="M14 11v6" /></template>
    <template v-else-if="name === 'users'"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" /></template>
    <template v-else-if="name === 'x'"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></template>
  </svg>
</template>

<style scoped>
.app-icon { display: inline-block; flex: 0 0 auto; }
</style>
