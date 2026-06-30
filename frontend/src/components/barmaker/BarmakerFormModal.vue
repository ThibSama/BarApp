<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue';
import AppIcon from '@/components/common/AppIcon.vue';

const props = withDefaults(defineProps<{
  open: boolean;
  title: string;
  eyebrow?: string;
  description?: string;
  size?: 'compact' | 'large';
  closeLabel?: string;
  closeDisabled?: boolean;
}>(), {
  eyebrow: 'CRÉATION',
  size: 'compact',
  closeLabel: 'Fermer la modale',
  closeDisabled: false,
});

const emit = defineEmits<{ close: [] }>();
const panel = ref<HTMLElement | null>(null);
const titleId = computed(() => `${props.title.toLocaleLowerCase('fr-FR').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, '-')}-modal-title`);
let previousFocus: Element | null = null;
let previousBodyOverflow = '';

function focusableElements(): HTMLElement[] {
  if (!panel.value) return [];
  return Array.from(panel.value.querySelectorAll<HTMLElement>('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'))
    .filter((element) => !element.hasAttribute('disabled') && element.getAttribute('aria-hidden') !== 'true');
}

function preferredInitialFocus(): HTMLElement | undefined {
  if (!panel.value) return undefined;
  return panel.value.querySelector<HTMLElement>('input, select, textarea') ?? focusableElements()[0];
}

function requestClose(): void {
  if (props.closeDisabled) return;
  emit('close');
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    event.preventDefault();
    requestClose();
    return;
  }
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
    previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    await nextTick();
    preferredInitialFocus()?.focus();
  } else {
    document.body.style.overflow = previousBodyOverflow;
    if (previousFocus instanceof HTMLElement) previousFocus.focus();
  }
}, { flush: 'post', immediate: true });

onUnmounted(() => {
  document.body.style.overflow = previousBodyOverflow;
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" role="presentation" @click.self="requestClose" @keydown="onKeydown">
      <section
        ref="panel"
        class="modal-panel"
        :class="`modal-panel--${size}`"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
      >
        <div class="modal-handle" aria-hidden="true"></div>
        <header class="modal-header">
          <div class="modal-heading">
            <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
            <h2 :id="titleId">{{ title }}</h2>
            <div class="title-underline" aria-hidden="true"></div>
            <p v-if="description" class="muted">{{ description }}</p>
          </div>
          <button class="modal-close" type="button" :aria-label="closeLabel" :disabled="closeDisabled" @click="requestClose">
            <AppIcon name="x" :size="20" />
          </button>
        </header>

        <div class="modal-body">
          <slot />
        </div>

        <footer v-if="$slots.footer" class="modal-footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; box-sizing: border-box; z-index: var(--z-modal-backdrop); display: flex; align-items: center; justify-content: center; padding: clamp(var(--space-4), 5vw, var(--space-8)); background: rgba(17, 17, 17, 0.58); backdrop-filter: blur(5px) saturate(0.9); }
.modal-panel { position: relative; box-sizing: border-box; z-index: var(--z-modal); width: min(520px, calc(100vw - 32px)); max-height: min(78vh, 720px); display: flex; flex-direction: column; overflow: hidden; border: 1px solid rgba(255,255,255,0.58); border-radius: 28px; background: var(--color-surface); box-shadow: 0 30px 90px rgba(17, 17, 17, 0.34), 0 8px 26px rgba(29, 43, 31, 0.14); animation: modal-enter 180ms ease-out; }
.modal-panel--compact { max-height: min(72vh, 520px); }
.modal-panel--large { width: min(860px, calc(100vw - 40px)); max-height: min(86vh, 820px); }
.modal-handle { display: none; }
.modal-header { flex: 0 0 auto; display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-4); padding: var(--space-5) var(--space-5) var(--space-3); background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(255,255,255,0.9)); }
.modal-heading { min-width: 0; }
.modal-heading h2 { margin: 0 0 var(--space-2); font-size: clamp(1.28rem, 2.2vw, 1.65rem); }
.modal-heading .muted { margin: 0; line-height: 1.45; }
.modal-heading .title-underline { margin-bottom: var(--space-2); }
.modal-close { flex: 0 0 auto; width: 42px; height: 42px; display: grid; place-items: center; border: 1px solid rgba(229,219,204,0.9); border-radius: var(--radius-round); background: rgba(255,255,255,0.86); color: var(--color-primary); cursor: pointer; box-shadow: 0 4px 14px rgba(29,43,31,0.06); }
.modal-close:disabled { opacity: 0.55; cursor: not-allowed; }
.modal-body { flex: 1 1 auto; min-height: 0; overflow-y: auto; overscroll-behavior: contain; padding: var(--space-5); background: linear-gradient(180deg, rgba(251,248,242,0.74), rgba(251,248,242,0.96)); }
.modal-panel--compact .modal-body { flex: 0 1 auto; }
.modal-footer { flex: 0 0 auto; display: flex; justify-content: flex-end; gap: var(--space-3); flex-wrap: wrap; padding: var(--space-4) var(--space-5) var(--space-5); background: rgba(255,255,255,0.94); }
@media (max-width: 820px) {
  .modal-backdrop { padding: var(--space-4); }
  .modal-panel--large { width: min(680px, calc(100vw - 32px)); max-height: 86dvh; }
}
@media (max-width: 560px) {
  .modal-backdrop { align-items: flex-end; justify-content: center; padding: var(--space-4) var(--space-3) calc(var(--space-4) + env(safe-area-inset-bottom)); }
  .modal-panel { width: calc(100vw - 24px); height: auto; max-height: 78dvh; border-radius: 26px; }
  .modal-panel--compact { max-height: 64dvh; }
  .modal-panel--large { width: calc(100vw - 24px); max-height: 80dvh; }
  .modal-handle { flex: 0 0 auto; display: block; width: 42px; height: 4px; margin: 10px auto 0; border-radius: var(--radius-round); background: rgba(29,43,31,0.24); }
  .modal-header { padding: var(--space-3) var(--space-4) var(--space-2); }
  .modal-heading h2 { margin-bottom: var(--space-2); font-size: 1.28rem; }
  .modal-heading .muted { font-size: 0.95rem; }
  .modal-body { padding: var(--space-3) var(--space-4) var(--space-4); }
  .modal-panel--large .modal-body { padding-bottom: var(--space-4); }
  .modal-footer { padding: var(--space-3) var(--space-4) var(--space-4); }
  .modal-footer :deep(.button) { flex: 1 1 12rem; }
  .modal-footer :deep(.button:not(.secondary)) { order: -1; }
}
@keyframes modal-enter { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@media (prefers-reduced-motion: reduce) { .modal-panel { animation: none; } }
</style>
