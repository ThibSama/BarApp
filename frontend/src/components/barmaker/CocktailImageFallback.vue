<script setup lang="ts">
import AppIcon from '@/components/common/AppIcon.vue';
withDefaults(defineProps<{ size?: 'small' | 'medium' | 'large' }>(), { size: 'medium' });
</script>

<template>
  <div class="cocktail-image-fallback" :class="size" aria-hidden="true">
    <AppIcon name="martini" :size="size === 'small' ? 24 : size === 'large' ? 54 : 34" />
  </div>
</template>

<style scoped>
.cocktail-image-fallback { display: grid; place-items: center; color: rgba(29,43,31,0.58); background: radial-gradient(circle at 50% 38%, rgba(212,175,55,0.22), transparent 34%), linear-gradient(135deg, #fbfaf7 0%, #efe8dd 100%); }
.small { width: 56px; height: 56px; border-radius: 14px; }
.medium { width: 76px; height: 76px; border-radius: 16px; }
.large { min-height: 164px; border-radius: var(--radius-large); }
</style>
