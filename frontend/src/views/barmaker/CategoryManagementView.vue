<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import BarmakerFormModal from "@/components/barmaker/BarmakerFormModal.vue";
import BarmakerPageHeader from "@/components/barmaker/BarmakerPageHeader.vue";
import ConfirmDialog from "@/components/barmaker/ConfirmDialog.vue";
import AppIcon from "@/components/common/AppIcon.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SuccessToast from "@/components/common/SuccessToast.vue";
import { useSuccessToast } from "@/composables/useSuccessToast";
import { useAdminCategoriesStore } from "@/stores/adminCategories";
import type { CategoryResponse } from "@/types/api";
import { describeAdminError, isConflict } from "@/utils/adminErrors";
import { validateRequired } from "@/utils/validation";

const store = useAdminCategoriesStore();
const { toastMessage, toastVisible, toastId, showSuccessToast } = useSuccessToast();

const form = reactive({ name: "", description: "", displayOrder: 0 });
const editingId = ref<number | null>(null);
const formOpen = ref(false);
const errors = reactive({ name: "", displayOrder: "" });
const formError = ref("");
const saving = ref(false);
const pendingDeactivate = ref<CategoryResponse | null>(null);

onMounted(() => store.load({ initial: true }));

const nextDisplayOrder = computed(
  () => store.items.reduce((max, category) => Math.max(max, category.displayOrder), -1) + 1,
);

function reset(): void {
  form.name = "";
  form.description = "";
  form.displayOrder = nextDisplayOrder.value;
  editingId.value = null;
  errors.name = "";
  errors.displayOrder = "";
  formError.value = "";
}
function openCreate(): void {
  reset();
  formOpen.value = true;
}
function closeForm(): void {
  if (saving.value) return;
  formOpen.value = false;
  reset();
}
function edit(category: CategoryResponse): void {
  reset();
  editingId.value = category.id;
  form.name = category.name;
  form.description = category.description ?? "";
  form.displayOrder = category.displayOrder;
  formOpen.value = true;
}

async function save(): Promise<void> {
  formError.value = "";
  errors.name = validateRequired(form.name, "Nom");
  errors.displayOrder =
    Number.isInteger(form.displayOrder) && form.displayOrder >= 0
      ? ""
      : "L’ordre d’affichage doit être positif ou nul.";
  if (errors.name || errors.displayOrder || saving.value) return;
  saving.value = true;
  try {
    if (editingId.value !== null) {
      const current = store.items.find((category) => category.id === editingId.value);
      await store.update(editingId.value, {
        name: form.name.trim(),
        description: form.description.trim() || null,
        displayOrder: form.displayOrder,
        active: current?.active ?? true,
      });
      showSuccessToast("Catégorie modifiée");
    } else {
      await store.create({
        name: form.name.trim(),
        description: form.description.trim() || null,
        displayOrder: form.displayOrder,
        active: true,
      });
      showSuccessToast("Catégorie créée");
    }
    formOpen.value = false;
    reset();
  } catch (err) {
    // Keep the modal open and preserve the typed input; surface field/general error.
    if (isConflict(err)) errors.name = "Une catégorie portant ce nom existe déjà.";
    else formError.value = describeAdminError(err);
  } finally {
    saving.value = false;
  }
}

async function toggleActive(category: CategoryResponse): Promise<void> {
  if (category.active) {
    pendingDeactivate.value = category;
    return;
  }
  try {
    await store.reactivate(category);
    showSuccessToast("Catégorie réactivée");
  } catch (err) {
    formError.value = describeAdminError(err);
  }
}

async function confirmDeactivate(): Promise<void> {
  const category = pendingDeactivate.value;
  pendingDeactivate.value = null;
  if (!category) return;
  try {
    await store.deactivate(category.id);
    showSuccessToast("Catégorie désactivée");
  } catch (err) {
    formError.value = describeAdminError(err);
  }
}
</script>

<template>
  <section class="stack category-page">
    <BarmakerPageHeader
      eyebrow="GESTION"
      title="Catégories"
      description="Organisez les familles de cocktails affichées sur la carte."
      action-label="Créer une catégorie"
      action-icon="plus"
      @action="openCreate" />

    <SuccessToast :message="toastMessage" :visible="toastVisible" :toast-key="toastId" />
    <p v-if="formError" class="alert error" role="alert">{{ formError }}</p>

    <section v-if="store.loading && !store.loaded" class="card empty-state" aria-busy="true">
      <h2>Chargement des catégories…</h2>
    </section>
    <section v-else-if="store.error && !store.items.length" class="card empty-state">
      <h2>Impossible de charger les catégories</h2>
      <p>{{ store.error }}</p>
      <button class="button" type="button" @click="store.load({ initial: true })">Réessayer</button>
    </section>

    <template v-else>
      <p v-if="store.error" class="alert warning" role="status">{{ store.error }}</p>
      <div v-if="store.items.length" class="category-grid">
        <article
          v-for="category in store.items"
          :key="category.id"
          class="category-card">
          <div class="card-heading">
            <span class="category-mark" aria-hidden="true"><AppIcon name="tags" :size="20" /></span>
            <StatusBadge
              :label="category.active ? 'Active' : 'Désactivée'"
              :tone="category.active ? 'success' : 'danger'" />
          </div>
          <div class="category-copy">
            <p class="eyebrow">Catégorie · ordre {{ category.displayOrder }}</p>
            <h2>{{ category.name }}</h2>
            <p>{{ category.description }}</p>
          </div>
          <div class="compact-actions">
            <button class="admin-action secondary" type="button" @click="edit(category)">
              <AppIcon name="pencil" :size="16" />Modifier
            </button>
            <button
              class="admin-action secondary"
              type="button"
              :disabled="store.isPending(category.id)"
              @click="toggleActive(category)">
              <AppIcon name="power" :size="16" />{{ category.active ? "Désactiver" : "Activer" }}
            </button>
          </div>
        </article>
      </div>
      <section v-else class="card empty-state">
        <h2>Aucune catégorie</h2>
        <p>Créez une première catégorie pour organiser la carte.</p>
      </section>
    </template>

    <BarmakerFormModal
      :open="formOpen"
      :eyebrow="editingId !== null ? 'MODIFICATION' : 'CRÉATION'"
      :title="editingId !== null ? 'Modifier une catégorie' : 'Créer une catégorie'"
      size="compact"
      close-label="Fermer le formulaire catégorie"
      :close-disabled="saving"
      @close="closeForm">
      <form id="category-form" class="category-form" @submit.prevent="save">
        <p v-if="formError" class="alert error" role="alert">{{ formError }}</p>
        <label>Nom <span aria-hidden="true">*</span><input v-model="form.name" type="text" /><span v-if="errors.name" class="field-error">{{ errors.name }}</span></label>
        <label>Description<textarea v-model="form.description" rows="4"></textarea></label>
        <label>Ordre d’affichage<input v-model.number="form.displayOrder" type="number" min="0" step="1" /><span v-if="errors.displayOrder" class="field-error">{{ errors.displayOrder }}</span></label>
      </form>
      <template #footer>
        <button class="button secondary" type="button" :disabled="saving" @click="closeForm">Annuler</button>
        <button class="button" type="submit" form="category-form" :disabled="saving">
          {{ saving ? "Enregistrement…" : editingId !== null ? "Enregistrer les modifications" : "Créer la catégorie" }}
        </button>
      </template>
    </BarmakerFormModal>

    <ConfirmDialog
      :open="Boolean(pendingDeactivate)"
      title="Désactiver la catégorie"
      :message="`Désactiver ${pendingDeactivate?.name ?? 'cette catégorie'} ? Elle restera visible ici mais disparaîtra de la carte client.`"
      confirm-label="Désactiver"
      @cancel="pendingDeactivate = null"
      @confirm="confirmDeactivate" />
  </section>
</template>

<style scoped>
.category-page { gap: 30px; }
.category-grid { display: grid; gap: var(--space-5); grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); }
.category-card { position: relative; display: grid; gap: var(--space-4); min-height: 250px; padding: 22px; border: 1px solid rgba(229, 219, 204, 0.9); border-radius: 22px; background: var(--color-surface); box-shadow: var(--shadow-card-soft); overflow: hidden; }
.category-card::before { content: ""; position: absolute; inset: 0 0 auto; height: 3px; background: var(--color-accent); opacity: 0.76; }
.card-heading { display: flex; justify-content: space-between; gap: var(--space-3); align-items: center; }
.category-mark { width: 44px; height: 44px; display: grid; place-items: center; border-radius: 14px; background: var(--color-surface-muted); color: var(--color-primary); }
.category-copy { display: grid; gap: var(--space-2); }
.category-copy h2 { margin: 0; font-size: 1.28rem; letter-spacing: -0.025em; }
.category-copy p:not(.eyebrow) { margin: 0; color: var(--color-text-secondary); line-height: 1.5; }
.compact-actions { display: flex; align-items: center; gap: var(--space-2); flex-wrap: wrap; margin-top: auto; }
.admin-action { display: inline-flex; align-items: center; gap: 7px; min-height: 40px; padding: 0 12px; border-radius: var(--radius-medium); font-weight: 800; cursor: pointer; }
.admin-action.secondary { border: 1px solid transparent; background: #f8f6f2; color: var(--color-primary); }
.admin-action.secondary:hover { background: var(--color-background-soft); }
.admin-action:disabled { opacity: 0.6; cursor: progress; }
.category-form { display: grid; gap: var(--space-4); padding: var(--space-1) 0; }
.category-form input, .category-form textarea { min-height: 50px; border-radius: 14px; }
.category-form textarea { min-height: 112px; resize: vertical; }
</style>
