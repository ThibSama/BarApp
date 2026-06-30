<script setup lang="ts">
import AppIcon from '@/components/common/AppIcon.vue';

defineProps<{
  eyebrow: string;
  title: string;
  description?: string;
  actionLabel?: string;
  actionTo?: string;
  actionIcon?: 'plus';
}>();

const emit = defineEmits<{ action: [] }>();
</script>

<template>
  <header class="barmaker-page-header">
    <div class="header-copy">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <div class="title-underline" aria-hidden="true"></div>
      <p v-if="description" class="muted">{{ description }}</p>
    </div>
    <RouterLink v-if="actionTo && actionLabel" class="button header-action" :to="actionTo">
      <AppIcon v-if="actionIcon" :name="actionIcon" :size="18" />
      {{ actionLabel }}
    </RouterLink>
    <button v-else-if="actionLabel" class="button header-action" type="button" @click="emit('action')">
      <AppIcon v-if="actionIcon" :name="actionIcon" :size="18" />
      {{ actionLabel }}
    </button>
  </header>
</template>

<style scoped>
.barmaker-page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-5); margin-bottom: var(--space-1); }
.header-copy { min-width: 0; max-width: 44rem; }
.header-copy h1 { margin-bottom: var(--space-3); }
.header-copy .muted { margin-bottom: 0; font-size: 1.02rem; line-height: 1.55; }
.header-action { flex: 0 0 auto; min-height: 48px; padding-inline: 18px; border-radius: 14px; box-shadow: 0 8px 18px rgba(29,43,31,0.12); }
@media (max-width: 720px) {
  .barmaker-page-header { display: grid; }
  .header-action { width: 100%; }
}
</style>
