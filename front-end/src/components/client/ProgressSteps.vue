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
    <li v-for="step in preparationSteps" :key="step" :class="{ done: isDone(step) }">
      <span aria-hidden="true"></span>{{ preparationStepLabels[step] }}
    </li>
  </ol>
</template>

<style scoped>
.progress-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 0.5rem; }
li { display: flex; align-items: center; gap: 0.5rem; color: #64748b; }
span { width: 0.85rem; height: 0.85rem; border-radius: 50%; border: 2px solid #cbd5e1; }
.done { color: #166534; font-weight: 700; }
.done span { background: #22c55e; border-color: #22c55e; }
</style>
