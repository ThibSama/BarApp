<script setup lang="ts">
import { reactive, ref } from 'vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useCatalogStore } from '@/stores/catalog';
import { validateRequired } from '@/utils/validation';
const catalog = useCatalogStore();
const form = reactive({ name: '', description: '' });
const editingId = ref('');
const errors = reactive({ name: '', description: '' });
const feedback = ref('');

function reset(): void { form.name = ''; form.description = ''; editingId.value = ''; errors.name = ''; errors.description = ''; }
function edit(id: string): void { const category = catalog.getCategoryById(id); if (category) { editingId.value = id; form.name = category.name; form.description = category.description; } }
function save(): void {
  errors.name = validateRequired(form.name, 'Nom'); errors.description = validateRequired(form.description, 'Description');
  if (errors.name || errors.description) return;
  if (editingId.value) { catalog.updateCategory(editingId.value, form); feedback.value = 'Catégorie modifiée.'; } else { catalog.createCategory(form.name, form.description); feedback.value = 'Catégorie créée.'; }
  reset();
}
function remove(id: string): void { if (window.confirm('Supprimer cette catégorie ? Les cocktails associés seront désactivés.')) { catalog.deleteCategory(id); feedback.value = 'Catégorie supprimée.'; } }
</script>

<template>
  <section class="stack">
    <div class="page-title"><div><p class="eyebrow">Gestion</p><h1>Catégories</h1></div></div>
    <form class="card form-grid" @submit.prevent="save"><h2>{{ editingId ? 'Modifier une catégorie' : 'Créer une catégorie' }}</h2><label>Nom<input v-model="form.name" type="text" /><span v-if="errors.name" class="field-error">{{ errors.name }}</span></label><label>Description<textarea v-model="form.description" rows="3"></textarea><span v-if="errors.description" class="field-error">{{ errors.description }}</span></label><div class="form-actions"><button class="button" type="submit">Enregistrer</button><button class="button secondary" type="button" @click="reset">Annuler</button></div></form>
    <p v-if="feedback" class="alert success">{{ feedback }}</p>
    <div class="card-grid two"><article v-for="category in catalog.categories" :key="category.id" class="card"><div class="card-heading"><h2>{{ category.name }}</h2><StatusBadge :label="category.enabled ? 'Active' : 'Désactivée'" :tone="category.enabled ? 'success' : 'danger'" /></div><p>{{ category.description }}</p><div class="form-actions"><button class="button secondary" type="button" @click="edit(category.id)">Modifier</button><button class="button secondary" type="button" @click="catalog.toggleCategory(category.id)">{{ category.enabled ? 'Désactiver' : 'Activer' }}</button><button class="button ghost" type="button" @click="remove(category.id)">Supprimer</button></div></article></div>
  </section>
</template>
