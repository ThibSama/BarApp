<script setup lang="ts">
import { reactive, ref } from "vue";
import BarmakerFormModal from "@/components/barmaker/BarmakerFormModal.vue";
import BarmakerPageHeader from "@/components/barmaker/BarmakerPageHeader.vue";
import ConfirmDialog from "@/components/barmaker/ConfirmDialog.vue";
import AppIcon from "@/components/common/AppIcon.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import { useCatalogStore } from "@/stores/catalog";
import { validateRequired } from "@/utils/validation";

const catalog = useCatalogStore();
const form = reactive({ name: "", description: "" });
const editingId = ref("");
const formOpen = ref(false);
const errors = reactive({ name: "", description: "" });
const feedback = ref("");
const formError = ref("");
const pendingDeleteId = ref("");

function reset(): void {
  form.name = "";
  form.description = "";
  editingId.value = "";
  errors.name = "";
  errors.description = "";
  formError.value = "";
}
function openCreate(): void {
  reset();
  editingId.value = "";
  formOpen.value = true;
}
function closeForm(): void {
  formOpen.value = false;
  reset();
}
function edit(id: string): void {
  const category = catalog.getCategoryById(id);
  if (category) {
    editingId.value = id;
    form.name = category.name;
    form.description = category.description;
    formOpen.value = true;
  }
}
function save(): void {
  formError.value = "";
  errors.name = validateRequired(form.name, "Nom");
  errors.description = validateRequired(form.description, "Description");
  if (errors.name || errors.description) return;
  try {
    if (editingId.value) {
      catalog.updateCategory(editingId.value, form);
      feedback.value = "Catégorie modifiée.";
    } else {
      catalog.createCategory(form.name, form.description);
      feedback.value = "Catégorie créée.";
    }
    closeForm();
  } catch {
    formError.value =
      "Impossible d’enregistrer cette catégorie pour le moment.";
  }
}
function confirmDelete(): void {
  if (!pendingDeleteId.value) return;
  try {
    catalog.deleteCategory(pendingDeleteId.value);
    feedback.value = "Catégorie supprimée.";
  } catch {
    feedback.value = "Impossible de supprimer cette catégorie.";
  }
  pendingDeleteId.value = "";
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

    <p v-if="feedback" class="alert success" role="status" aria-live="polite">
      {{ feedback }}
    </p>

    <div v-if="catalog.categories.length" class="category-grid">
      <article
        v-for="category in catalog.categories"
        :key="category.id"
        class="category-card">
        <div class="card-heading">
          <span class="category-mark" aria-hidden="true"
            ><AppIcon name="tags" :size="20"
          /></span>
          <StatusBadge
            :label="category.enabled ? 'Active' : 'Désactivée'"
            :tone="category.enabled ? 'success' : 'danger'" />
        </div>
        <div class="category-copy">
          <p class="eyebrow">Catégorie</p>
          <h2>{{ category.name }}</h2>
          <p>{{ category.description }}</p>
        </div>
        <div class="compact-actions">
          <button
            class="admin-action secondary"
            type="button"
            @click="edit(category.id)">
            <AppIcon name="pencil" :size="16" />Modifier
          </button>
          <button
            class="admin-action secondary"
            type="button"
            @click="catalog.toggleCategory(category.id)">
            <AppIcon name="power" :size="16" />{{
              category.enabled ? "Désactiver" : "Activer"
            }}
          </button>
          <button
            class="icon-button danger"
            type="button"
            :aria-label="`Supprimer ${category.name}`"
            @click="pendingDeleteId = category.id">
            <AppIcon name="trash" :size="18" />
          </button>
        </div>
      </article>
    </div>
    <section v-else class="card empty-state">
      <h2>Aucune catégorie</h2>
      <p>Créez une première catégorie pour organiser la carte.</p>
    </section>

    <BarmakerFormModal
      :open="formOpen"
      :eyebrow="editingId ? 'MODIFICATION' : 'CRÉATION'"
      :title="editingId ? 'Modifier une catégorie' : 'Créer une catégorie'"
      size="compact"
      close-label="Fermer le formulaire catégorie"
      @close="closeForm">
      <form id="category-form" class="category-form" @submit.prevent="save">
        <p v-if="formError" class="alert error" role="alert">{{ formError }}</p>
        <label
          >Nom <span aria-hidden="true">*</span
          ><input v-model="form.name" type="text" /><span
            v-if="errors.name"
            class="field-error"
            >{{ errors.name }}</span
          ></label
        >
        <label
          >Description <span aria-hidden="true">*</span
          ><textarea v-model="form.description" rows="4"></textarea
          ><span v-if="errors.description" class="field-error">{{
            errors.description
          }}</span></label
        >
      </form>
      <template #footer>
        <button class="button secondary" type="button" @click="closeForm">
          Annuler
        </button>
        <button class="button" type="submit" form="category-form">
          {{
            editingId ? "Enregistrer les modifications" : "Créer la catégorie"
          }}
        </button>
      </template>
    </BarmakerFormModal>

    <ConfirmDialog
      :open="Boolean(pendingDeleteId)"
      title="Supprimer la catégorie"
      :message="`Supprimer ${catalog.getCategoryById(pendingDeleteId)?.name ?? 'cette catégorie'} ? Les cocktails associés seront désactivés.`"
      confirm-label="Supprimer"
      @cancel="pendingDeleteId = ''"
      @confirm="confirmDelete" />
  </section>
</template>

<style scoped>
.category-page {
  gap: 30px;
}
.category-grid {
  display: grid;
  gap: var(--space-5);
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}
.category-card {
  position: relative;
  display: grid;
  gap: var(--space-4);
  min-height: 250px;
  padding: 22px;
  border: 1px solid rgba(229, 219, 204, 0.9);
  border-radius: 22px;
  background: var(--color-surface);
  box-shadow: var(--shadow-card-soft);
  overflow: hidden;
}
.category-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: var(--color-accent);
  opacity: 0.76;
}
.card-heading {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  align-items: center;
}
.category-mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: var(--color-surface-muted);
  color: var(--color-primary);
}
.category-copy {
  display: grid;
  gap: var(--space-2);
}
.category-copy h2 {
  margin: 0;
  font-size: 1.28rem;
  letter-spacing: -0.025em;
}
.category-copy p:not(.eyebrow) {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.compact-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
  margin-top: auto;
}
.admin-action {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 12px;
  border-radius: var(--radius-medium);
  font-weight: 800;
  cursor: pointer;
}
.admin-action.secondary {
  border: 1px solid transparent;
  background: #f8f6f2;
  color: var(--color-primary);
}
.admin-action.secondary:hover {
  background: var(--color-background-soft);
}
.icon-button {
  width: 44px;
  height: 44px;
  display: inline-grid;
  place-items: center;
  border: 0;
  border-radius: var(--radius-round);
  background: #f8f6f2;
  color: var(--color-primary);
  cursor: pointer;
}
.icon-button.danger:hover {
  background: #fde8e7;
  color: var(--color-error);
}
.category-form {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-1) 0;
}
.category-form input,
.category-form textarea {
  min-height: 50px;
  border-radius: 14px;
}
.category-form textarea {
  min-height: 112px;
  resize: vertical;
}
</style>
