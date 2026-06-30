<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';

const props = defineProps<{
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
}>();

const emit = defineEmits<{ cancel: []; confirm: [] }>();

const panel = ref<HTMLElement | null>(null);
let previousFocus: Element | null = null;

function close(): void {
  emit('cancel');
}

function focusableElements(): HTMLElement[] {
  if (!panel.value) return [];
  return Array.from(panel.value.querySelectorAll<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')).filter((element) => !element.hasAttribute('disabled'));
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') close();
  if (event.key !== 'Tab') return;
  const focusable = focusableElements();
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

watch(() => props.open, async (isOpen) => {
  if (isOpen) {
    previousFocus = document.activeElement;
    await nextTick();
    focusableElements()[0]?.focus();
  } else if (previousFocus instanceof HTMLElement) {
    previousFocus.focus();
  }
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-backdrop" role="presentation" @click.self="close" @keydown="onKeydown">
      <section ref="panel" class="dialog-panel" role="dialog" aria-modal="true" :aria-labelledby="`${title}-title`">
        <p class="eyebrow">Confirmation</p>
        <h2 :id="`${title}-title`">{{ title }}</h2>
        <p class="muted">{{ message }}</p>
        <div class="dialog-actions">
          <button class="button secondary" type="button" @click="close">Annuler</button>
          <button class="button ghost danger-action" type="button" @click="emit('confirm')">{{ confirmLabel ?? 'Confirmer' }}</button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-backdrop { position: fixed; inset: 0; z-index: 100; display: grid; place-items: center; padding: var(--space-4); background: rgba(17,17,17,0.34); }
.dialog-panel { width: min(440px, 100%); display: grid; gap: var(--space-4); padding: var(--space-6); border: 1px solid var(--color-border); border-radius: var(--radius-large); background: #fff; box-shadow: 0 24px 70px rgba(0,0,0,0.18); }
.dialog-panel h2 { margin: 0; }
.dialog-actions { display: flex; justify-content: flex-end; gap: var(--space-3); flex-wrap: wrap; }
.danger-action { border-color: #f0c7c3; }
</style>
