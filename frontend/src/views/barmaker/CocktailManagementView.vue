<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BarmakerCocktailForm from '@/components/barmaker/BarmakerCocktailForm.vue';
import BarmakerFormModal from '@/components/barmaker/BarmakerFormModal.vue';
import BarmakerPageHeader from '@/components/barmaker/BarmakerPageHeader.vue';
import ConfirmDialog from '@/components/barmaker/ConfirmDialog.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import CocktailImage from '@/components/common/CocktailImage.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import SuccessToast from '@/components/common/SuccessToast.vue';
import { useSuccessToast } from '@/composables/useSuccessToast';
import { useAdminCocktailsStore } from '@/stores/adminCocktails';
import { useAdminCategoriesStore } from '@/stores/adminCategories';
import type { CocktailResponse } from '@/types/api';
import { describeAdminError } from '@/utils/adminErrors';
import { formatCurrency } from '@/utils/formatters';
import { priceForSize } from '@/utils/menu';

const store = useAdminCocktailsStore();
const categories = useAdminCategoriesStore();
const route = useRoute();
const router = useRouter();
const { toastMessage, toastVisible, toastId, showSuccessToast } = useSuccessToast();

const selectedCategory = ref<number | 'all'>('all');
const availability = ref<'all' | 'available' | 'unavailable'>('all');
const search = ref('');
const pendingDeactivate = ref<CocktailResponse | null>(null);
const actionError = ref('');
const formModalKey = ref(0);
const formSubmitting = ref(false);
const cocktailForm = ref<InstanceType<typeof BarmakerCocktailForm> | null>(null);

onMounted(() => {
  store.load({ initial: true });
  if (!categories.loaded) categories.load({ initial: true });
});

const formMode = computed<'create' | 'edit' | null>(() => {
  if (route.query.modal === 'create') return 'create';
  if (route.query.modal === 'edit' && typeof route.query.cocktailId === 'string') return 'edit';
  return null;
});
const editingCocktailId = computed(() =>
  formMode.value === 'edit' && typeof route.query.cocktailId === 'string'
    ? Number(route.query.cocktailId)
    : undefined,
);
const formModalOpen = computed(() => formMode.value === 'create' || formMode.value === 'edit');
const formModalTitle = computed(() => (formMode.value === 'edit' ? 'Modifier le cocktail' : 'Créer un cocktail'));
const formModalEyebrow = computed(() => (formMode.value === 'edit' ? 'MODIFICATION' : 'CRÉATION'));

const filteredCocktails = computed(() =>
  store.items.filter((cocktail) => {
    const matchesCategory = selectedCategory.value === 'all' || cocktail.categoryId === selectedCategory.value;
    const matchesAvailability =
      availability.value === 'all' || (availability.value === 'available' ? cocktail.active : !cocktail.active);
    const term = search.value.trim().toLocaleLowerCase('fr-FR');
    const matchesSearch = !term || cocktail.name.toLocaleLowerCase('fr-FR').includes(term);
    return matchesCategory && matchesAvailability && matchesSearch;
  }),
);

function clearFilters(): void {
  selectedCategory.value = 'all';
  availability.value = 'all';
  search.value = '';
}
function openCreateModal(): void {
  formModalKey.value += 1;
  router.push({ name: 'bar-cocktails', query: { ...route.query, modal: 'create', cocktailId: undefined } });
}
function openEditModal(cocktailId: number): void {
  formModalKey.value += 1;
  router.push({ name: 'bar-cocktails', query: { ...route.query, modal: 'edit', cocktailId: String(cocktailId) } });
}
function closeFormModal(force = false): void {
  if (formSubmitting.value && !force) return;
  const query = { ...route.query };
  delete query.modal;
  delete query.cocktailId;
  router.push({ name: 'bar-cocktails', query });
}
function onSaved(): void {
  showSuccessToast(formMode.value === 'edit' ? 'Cocktail modifié' : 'Cocktail créé');
  closeFormModal(true);
}

async function toggleActive(cocktail: CocktailResponse): Promise<void> {
  actionError.value = '';
  if (cocktail.active) {
    pendingDeactivate.value = cocktail;
    return;
  }
  try {
    await store.reactivate(cocktail);
    showSuccessToast('Cocktail réactivé');
  } catch (err) {
    actionError.value = describeAdminError(err);
  }
}

async function confirmDeactivate(): Promise<void> {
  const cocktail = pendingDeactivate.value;
  pendingDeactivate.value = null;
  if (!cocktail) return;
  try {
    await store.deactivate(cocktail.id);
    showSuccessToast('Cocktail désactivé');
  } catch (err) {
    actionError.value = describeAdminError(err);
  }
}

watch(() => [route.query.modal, route.query.cocktailId], () => { formModalKey.value += 1; });
</script>

<template>
  <section class="stack cocktail-page">
    <BarmakerPageHeader eyebrow="GESTION" title="Cocktails" description="Gérez les recettes, les prix et les disponibilités de la carte." action-label="Créer un cocktail" action-icon="plus" @action="openCreateModal" />

    <SuccessToast :message="toastMessage" :visible="toastVisible" :toast-key="toastId" />
    <p v-if="actionError" class="alert error" role="alert">{{ actionError }}</p>

    <section class="toolbar" aria-label="Filtres cocktails">
      <label class="search-field">Recherche<span class="input-wrap"><AppIcon name="search" :size="20" /><input v-model="search" type="search" placeholder="Nom du cocktail" /></span></label>
      <label>Catégorie<select v-model="selectedCategory"><option value="all">Toutes les catégories</option><option v-for="category in categories.items" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
      <label>Disponibilité<select v-model="availability"><option value="all">Tous</option><option value="available">Disponible</option><option value="unavailable">Indisponible</option></select></label>
      <button class="reset-action" type="button" @click="clearFilters"><AppIcon name="sliders" :size="17" />Réinitialiser</button>
    </section>

    <section v-if="store.loading && !store.loaded" class="card empty-state" aria-busy="true"><h2>Chargement des cocktails…</h2></section>
    <section v-else-if="store.error && !store.items.length" class="card empty-state">
      <h2>Impossible de charger les cocktails</h2>
      <p>{{ store.error }}</p>
      <button class="button" type="button" @click="store.load({ initial: true })">Réessayer</button>
    </section>

    <template v-else>
      <p v-if="store.error" class="alert warning" role="status">{{ store.error }}</p>
      <div v-if="filteredCocktails.length" class="cocktail-list">
        <article v-for="cocktail in filteredCocktails" :key="cocktail.id" class="cocktail-row">
          <div class="thumb"><CocktailImage :image-url="cocktail.imageUrl ?? undefined" :cocktail-name="cocktail.name" /></div>
          <div class="cocktail-copy"><h2>{{ cocktail.name }}</h2><p>{{ cocktail.shortDescription }}</p><p class="category-meta">{{ cocktail.categoryName }}</p></div>
          <dl class="prices"><div><dt>S</dt><dd>{{ formatCurrency(priceForSize(cocktail.prices, 'S') ?? 0) }}</dd></div><div><dt>M</dt><dd>{{ formatCurrency(priceForSize(cocktail.prices, 'M') ?? 0) }}</dd></div><div><dt>L</dt><dd>{{ formatCurrency(priceForSize(cocktail.prices, 'L') ?? 0) }}</dd></div></dl>
          <StatusBadge :label="cocktail.active ? 'Disponible' : 'Indisponible'" :tone="cocktail.active ? 'success' : 'danger'" />
          <div class="row-actions"><button class="admin-action secondary" type="button" @click="openEditModal(cocktail.id)"><AppIcon name="pencil" :size="16" />Modifier</button><button class="admin-action secondary" type="button" :disabled="store.isPending(cocktail.id)" @click="toggleActive(cocktail)"><AppIcon name="power" :size="16" />{{ cocktail.active ? 'Désactiver' : 'Activer' }}</button></div>
        </article>
      </div>
      <section v-else class="card empty-state"><h2>Aucun cocktail</h2><p>Créez un nouveau cocktail ou modifiez les filtres.</p></section>
    </template>

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
        :cocktail-id="editingCocktailId"
        form-id="cocktail-modal-form"
        :show-footer="false"
        @saved="onSaved"
        @submitting-change="formSubmitting = $event"
      />
      <template #footer>
        <button class="button secondary" type="button" :disabled="formSubmitting" @click="closeFormModal()">Annuler</button>
        <button class="button" type="button" :disabled="formSubmitting" @click="cocktailForm?.save()">{{ formSubmitting ? 'Enregistrement…' : 'Enregistrer' }}</button>
      </template>
    </BarmakerFormModal>

    <ConfirmDialog :open="Boolean(pendingDeactivate)" title="Désactiver le cocktail" :message="`Désactiver ${pendingDeactivate?.name ?? 'ce cocktail'} ? Il restera visible ici mais disparaîtra de la carte client.`" confirm-label="Désactiver" @cancel="pendingDeactivate = null" @confirm="confirmDeactivate" />
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
.admin-action:disabled { opacity: 0.6; cursor: progress; }
@media (min-width: 760px) { .toolbar { grid-template-columns: minmax(220px, 1.2fr) 1fr 1fr auto; } }
@media (min-width: 1100px) { .cocktail-row { grid-template-columns: 76px minmax(0, 1fr) auto auto auto; } }
@media (max-width: 700px) { .cocktail-row { margin: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-large); } .cocktail-list { border: 0; background: transparent; box-shadow: none; } .row-actions { justify-content: flex-start; } }
</style>
