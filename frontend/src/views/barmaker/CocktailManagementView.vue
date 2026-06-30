<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BarmakerCocktailForm from '@/components/barmaker/BarmakerCocktailForm.vue';
import BarmakerFormModal from '@/components/barmaker/BarmakerFormModal.vue';
import BarmakerPageHeader from '@/components/barmaker/BarmakerPageHeader.vue';
import ConfirmDialog from '@/components/barmaker/ConfirmDialog.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import CocktailImage from '@/components/common/CocktailImage.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useCatalogStore } from '@/stores/catalog';
import { formatCurrency } from '@/utils/formatters';

const catalog = useCatalogStore();
const route = useRoute();
const router = useRouter();
const selectedCategory = ref('all');
const availability = ref<'all' | 'available' | 'unavailable'>('all');
const search = ref('');
const pendingDeleteId = ref('');
const formModalKey = ref(0);
const formSubmitting = ref(false);
const cocktailForm = ref<InstanceType<typeof BarmakerCocktailForm> | null>(null);

const formMode = computed<'create' | 'edit' | null>(() => {
  if (route.query.modal === 'create') return 'create';
  if (route.query.modal === 'edit' && typeof route.query.cocktailId === 'string') return 'edit';
  return null;
});
const editingCocktailId = computed(() => formMode.value === 'edit' && typeof route.query.cocktailId === 'string' ? route.query.cocktailId : '');
const editingCocktail = computed(() => editingCocktailId.value ? catalog.getCocktailById(editingCocktailId.value) : undefined);
const formModalOpen = computed(() => formMode.value === 'create' || Boolean(editingCocktail.value));
const formModalTitle = computed(() => formMode.value === 'edit' ? 'Modifier le cocktail' : 'Créer un cocktail');
const formModalEyebrow = computed(() => formMode.value === 'edit' ? 'MODIFICATION' : 'CRÉATION');

const filteredCocktails = computed(() => catalog.cocktails.filter((cocktail) => {
  const matchesCategory = selectedCategory.value === 'all' || cocktail.categoryId === selectedCategory.value;
  const matchesAvailability = availability.value === 'all' || (availability.value === 'available' ? cocktail.available : !cocktail.available);
  const term = search.value.trim().toLocaleLowerCase('fr-FR');
  const matchesSearch = !term || cocktail.name.toLocaleLowerCase('fr-FR').includes(term);
  return matchesCategory && matchesAvailability && matchesSearch;
}));

function clearFilters(): void { selectedCategory.value = 'all'; availability.value = 'all'; search.value = ''; }
function confirmDelete(): void { if (pendingDeleteId.value) catalog.deleteCocktail(pendingDeleteId.value); pendingDeleteId.value = ''; }
function openCreateModal(): void { formModalKey.value += 1; router.push({ name: 'bar-cocktails', query: { ...route.query, modal: 'create', cocktailId: undefined } }); }
function openEditModal(cocktailId: string): void { formModalKey.value += 1; router.push({ name: 'bar-cocktails', query: { ...route.query, modal: 'edit', cocktailId } }); }
function closeFormModal(force = false): void {
  if (formSubmitting.value && !force) return;
  const query = { ...route.query };
  delete query.modal;
  delete query.cocktailId;
  router.push({ name: 'bar-cocktails', query });
}
function onSaved(): void { closeFormModal(true); }

watch(() => [route.query.modal, route.query.cocktailId], () => { formModalKey.value += 1; });
</script>

<template>
  <section class="stack cocktail-page">
    <BarmakerPageHeader eyebrow="GESTION" title="Cocktails" description="Gérez les recettes, les prix et les disponibilités de la carte." action-label="Créer un cocktail" action-icon="plus" @action="openCreateModal" />

    <section class="toolbar" aria-label="Filtres cocktails">
      <label class="search-field">Recherche<span class="input-wrap"><AppIcon name="search" :size="20" /><input v-model="search" type="search" placeholder="Nom du cocktail" /></span></label>
      <label>Catégorie<select v-model="selectedCategory"><option value="all">Toutes les catégories</option><option v-for="category in catalog.categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
      <label>Disponibilité<select v-model="availability"><option value="all">Tous</option><option value="available">Disponible</option><option value="unavailable">Indisponible</option></select></label>
      <button class="reset-action" type="button" @click="clearFilters"><AppIcon name="sliders" :size="17" />Réinitialiser</button>
    </section>

    <div v-if="filteredCocktails.length" class="cocktail-list">
      <article v-for="cocktail in filteredCocktails" :key="cocktail.id" class="cocktail-row">
        <div class="thumb"><CocktailImage :image-url="cocktail.imageUrl" :cocktail-name="cocktail.name" /></div>
        <div class="cocktail-copy"><h2>{{ cocktail.name }}</h2><p>{{ cocktail.shortDescription }}</p><p class="category-meta">{{ catalog.getCategoryById(cocktail.categoryId)?.name ?? 'Catégorie' }}</p></div>
        <dl class="prices"><div><dt>S</dt><dd>{{ formatCurrency(cocktail.prices.S) }}</dd></div><div><dt>M</dt><dd>{{ formatCurrency(cocktail.prices.M) }}</dd></div><div><dt>L</dt><dd>{{ formatCurrency(cocktail.prices.L) }}</dd></div></dl>
        <StatusBadge :label="cocktail.available ? 'Disponible' : 'Indisponible'" :tone="cocktail.available ? 'success' : 'danger'" />
        <div class="row-actions"><button class="admin-action secondary" type="button" @click="openEditModal(cocktail.id)"><AppIcon name="pencil" :size="16" />Modifier</button><button class="admin-action secondary" type="button" @click="catalog.toggleCocktail(cocktail.id)"><AppIcon name="power" :size="16" />{{ cocktail.available ? 'Désactiver' : 'Activer' }}</button><button class="icon-button danger" type="button" :aria-label="`Supprimer ${cocktail.name}`" @click="pendingDeleteId = cocktail.id"><AppIcon name="trash" :size="18" /></button></div>
      </article>
    </div>
    <section v-else class="card empty-state"><h2>Aucun cocktail</h2><p>Créez un nouveau cocktail ou modifiez les filtres.</p></section>

    <BarmakerFormModal
      :open="formModalOpen"
      :title="formModalTitle"
      :eyebrow="formModalEyebrow"
      description="Renseignez uniquement les champs réellement utilisés par l’application."
      size="large"
      close-label="Fermer le formulaire cocktail"
      :close-disabled="formSubmitting"
      @close="closeFormModal"
    >
      <BarmakerCocktailForm
        ref="cocktailForm"
        :key="formModalKey"
        :cocktail-id="editingCocktailId || undefined"
        form-id="cocktail-modal-form"
        :show-footer="false"
        @saved="onSaved"
        @submitting-change="formSubmitting = $event"
      />
      <template #footer>
        <button class="button secondary" type="button" :disabled="formSubmitting" @click="closeFormModal">Annuler</button>
        <button class="button" type="button" :disabled="formSubmitting" @click="cocktailForm?.save()">{{ formSubmitting ? 'Enregistrement…' : 'Enregistrer' }}</button>
      </template>
    </BarmakerFormModal>

    <ConfirmDialog :open="Boolean(pendingDeleteId)" title="Supprimer le cocktail" :message="`Supprimer ${catalog.getCocktailById(pendingDeleteId)?.name ?? 'ce cocktail'} ? Cette action retirera la recette de la gestion.`" confirm-label="Supprimer" @cancel="pendingDeleteId = ''" @confirm="confirmDelete" />
  </section>
</template>

<style scoped>
.cocktail-page { gap: 30px; }
.toolbar { display: grid; gap: var(--space-4); align-items: end; padding: var(--space-5); border: 1px solid rgba(229,219,204,0.84); border-radius: 24px; background: var(--color-panel); box-shadow: inset 0 1px 0 rgba(255,255,255,0.64); }
.input-wrap { position: relative; display: flex; align-items: center; }
.input-wrap .app-icon { position: absolute; left: 16px; color: var(--color-text-secondary); }
.input-wrap input { padding-left: 48px; }
.toolbar input, .toolbar select { min-height: 54px; border-radius: 14px; }
.reset-action { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-2); min-height: 54px; padding: 0 16px; border: 1px solid var(--color-border); border-radius: 14px; background: #fff; color: var(--color-primary); font-weight: 800; cursor: pointer; }
.reset-action:hover { background: var(--color-background-soft); }
.cocktail-list { overflow: hidden; border: 1px solid rgba(229,219,204,0.9); border-radius: 22px; background: #fff; box-shadow: var(--shadow-card); }
.cocktail-row { position: relative; display: grid; gap: var(--space-4); align-items: center; padding: 18px 20px 18px 24px; border-bottom: 1px solid var(--color-border); }
.cocktail-row::before { content: ''; position: absolute; left: 0; top: 18px; bottom: 18px; width: 3px; border-radius: var(--radius-round); background: var(--color-accent); opacity: 0.62; }
.cocktail-row:last-child { border-bottom: 0; }
.thumb { width: 76px; height: 76px; overflow: hidden; border-radius: 16px; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.cocktail-copy h2 { margin: 0 0 5px; font-size: 1.14rem; letter-spacing: -0.025em; }
.cocktail-copy p { margin: 0; color: var(--color-text-secondary); line-height: 1.42; }
.category-meta { margin-top: var(--space-1) !important; font-size: 0.85rem; font-weight: 750; color: var(--color-primary) !important; }
.prices { display: grid; grid-template-columns: repeat(3, auto); gap: var(--space-3); margin: 0; color: var(--color-text-secondary); }
.prices div { display: grid; gap: 2px; }
.prices dt { font-size: 0.72rem; font-weight: 900; color: var(--color-primary); }
.prices dd { margin: 0; font-weight: 750; white-space: nowrap; }
.row-actions { display: flex; gap: var(--space-2); flex-wrap: wrap; justify-content: flex-end; }
.admin-action { display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 40px; padding: 0 12px; border-radius: var(--radius-medium); font-weight: 800; cursor: pointer; }
.admin-action.secondary { border: 1px solid transparent; background: var(--color-surface-muted); color: var(--color-primary); }
.admin-action.secondary:hover { background: var(--color-background-soft); text-decoration: none; }
.icon-button { width: 44px; height: 44px; display: inline-grid; place-items: center; border: 0; border-radius: var(--radius-round); background: #f8f6f2; color: var(--color-primary); cursor: pointer; }
.icon-button.danger:hover { background: #fde8e7; color: var(--color-error); }
@media (min-width: 760px) { .toolbar { grid-template-columns: minmax(220px, 1.2fr) 1fr 1fr auto; } }
@media (min-width: 1100px) { .cocktail-row { grid-template-columns: 76px minmax(0, 1fr) auto auto auto; } }
@media (max-width: 700px) { .cocktail-row { margin: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-large); } .cocktail-list { border: 0; background: transparent; box-shadow: none; } .row-actions { justify-content: flex-start; } }
</style>
