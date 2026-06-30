<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BarmakerPageHeader from '@/components/barmaker/BarmakerPageHeader.vue';
import BarmakerCocktailForm from '@/components/barmaker/BarmakerCocktailForm.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { useCatalogStore } from '@/stores/catalog';

const route = useRoute();
const router = useRouter();
const catalog = useCatalogStore();
const cocktailId = computed(() => String(route.params.cocktailId ?? ''));
const existing = computed(() => cocktailId.value ? catalog.getCocktailById(cocktailId.value) : undefined);
const title = computed(() => existing.value ? 'Modifier le cocktail' : 'Créer un cocktail');

function close(): void {
  router.push('/bar/cocktails');
}
</script>

<template>
  <section class="stack cocktail-form-page">
    <RouterLink class="back-link" to="/bar/cocktails"><AppIcon name="arrow-left" :size="18" />Retour aux cocktails</RouterLink>
    <BarmakerPageHeader eyebrow="GESTION" :title="title" description="Renseignez uniquement les champs réellement utilisés par l’application." />
    <BarmakerCocktailForm :cocktail-id="cocktailId" form-id="cocktail-edit-form" @saved="close" @cancel="close" />
  </section>
</template>

<style scoped>
.cocktail-form-page { gap: 30px; }
.back-link { width: fit-content; display: inline-flex; align-items: center; gap: var(--space-2); color: var(--color-primary); }
</style>
