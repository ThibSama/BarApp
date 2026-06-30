<script setup lang="ts">
import type { PreparationStep } from '@/types/domain';
import { preparationStepLabels, preparationSteps } from '@/utils/formatters';

const props = defineProps<{ currentStep: PreparationStep }>();
function isDone(step: PreparationStep): boolean {
  return preparationSteps.indexOf(step) <= preparationSteps.indexOf(props.currentStep);
}
</script>

<template>
  <ol class="progress-list">
    <li v-for="step in preparationSteps" :key="step" :class="{ done: isDone(step), current: step === currentStep }" :aria-current="step === currentStep ? 'step' : undefined">
      <span aria-hidden="true"></span>{{ preparationStepLabels[step] }}
    </li>
  </ol>
</template>

<style scoped>
.progress-list { list-style: none; padding: 0; margin: 0; display: grid; gap: var(--space-3); }
li { display: flex; align-items: center; gap: var(--space-2); color: var(--color-text-secondary); }
span { width: 0.9rem; height: 0.9rem; border-radius: 50%; border: 2px solid var(--color-border); flex: 0 0 auto; }
.done { color: var(--color-success); font-weight: 700; }
.done span { background: var(--color-success); border-color: var(--color-success); }
.current { color: var(--color-primary); }
.current span { background: var(--color-accent); border-color: var(--color-accent); box-shadow: 0 0 0 4px rgba(212,175,55,0.16); }
</style>
