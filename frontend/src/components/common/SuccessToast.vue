<script setup lang="ts">
import AppIcon from '@/components/common/AppIcon.vue';

withDefaults(defineProps<{
  message: string;
  visible: boolean;
  toastKey?: number;
}>(), {
  toastKey: 0,
});
</script>

<template>
  <Teleport to="body">
    <Transition name="success-toast">
      <div v-if="visible && message" class="success-toast" role="status" aria-live="polite">
        <span class="success-toast__icon" aria-hidden="true"><AppIcon name="check" :size="18" /></span>
        <span class="success-toast__message">{{ message }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.success-toast {
  position: fixed;
  z-index: 80;
  right: var(--space-6);
  bottom: var(--space-6);
  width: min(420px, calc(100vw - 2 * var(--space-4)));
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid rgba(46, 125, 50, 0.18);
  border-radius: var(--radius-large);
  background: rgba(255, 255, 255, 0.96);
  color: var(--color-primary);
  box-shadow: var(--shadow-card), 0 8px 28px rgba(46, 125, 50, 0.12);
  backdrop-filter: blur(12px);
}
.success-toast__icon {
  width: 32px;
  height: 32px;
  display: inline-grid;
  place-items: center;
  border-radius: var(--radius-round);
  background: var(--color-success);
  color: #fff;
}
.success-toast__message {
  min-width: 0;
  font-weight: 800;
  line-height: 1.35;
}
.success-toast-enter-active,
.success-toast-leave-active { transition: opacity 180ms ease, transform 180ms ease; }
.success-toast-enter-from,
.success-toast-leave-to { opacity: 0; transform: translateY(12px) scale(0.98); }
@media (max-width: 720px) {
  .success-toast {
    left: var(--space-4);
    right: var(--space-4);
    bottom: calc(6rem + env(safe-area-inset-bottom));
    width: auto;
    padding: var(--space-3) var(--space-4);
  }
}
@media (min-width: 960px) {
  .success-toast { right: var(--space-8); bottom: var(--space-8); }
}
@media (prefers-reduced-motion: reduce) {
  .success-toast-enter-active,
  .success-toast-leave-active { transition: none; }
}
</style>
