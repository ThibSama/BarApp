<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { cocktailImageAlt, cocktailPlaceholderImage, resolveCocktailImageSrc } from '@/utils/cocktailImages';

const props = defineProps<{
  imageUrl?: string;
  cocktailName?: string;
  alt?: string;
}>();

const failed = ref(false);

watch(() => props.imageUrl, () => { failed.value = false; });

const src = computed(() => resolveCocktailImageSrc(props.imageUrl, failed.value));
const accessibleAlt = computed(() => props.alt ?? cocktailImageAlt(props.cocktailName));

function onError(): void {
  if (src.value !== cocktailPlaceholderImage) failed.value = true;
}
</script>

<template>
  <img class="cocktail-image" :src="src" :alt="accessibleAlt" @error="onError" />
</template>

<style scoped>
.cocktail-image { display: block; width: 100%; height: 100%; object-fit: cover; object-position: center; }
</style>
