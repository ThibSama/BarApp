<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import BarmakerFormModal from '@/components/barmaker/BarmakerFormModal.vue';
import BarmakerPageHeader from '@/components/barmaker/BarmakerPageHeader.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import SuccessToast from '@/components/common/SuccessToast.vue';
import { useSuccessToast } from '@/composables/useSuccessToast';
import { useAdminUsersStore } from '@/stores/adminUsers';
import { describeAdminError, isConflict } from '@/utils/adminErrors';
import { validateRequired } from '@/utils/validation';

const store = useAdminUsersStore();
const { toastMessage, toastVisible, toastId, showSuccessToast } = useSuccessToast();

const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;

const form = reactive({ displayName: '', username: '', password: '', passwordConfirmation: '' });
const errors = reactive({ displayName: '', username: '', password: '', passwordConfirmation: '' });
const formOpen = ref(false);
const formError = ref('');
const saving = ref(false);

onMounted(() => store.load({ initial: true }));

function roleLabel(role: string): string {
  return role === 'MANAGER' ? 'Manager' : 'Barmaker';
}

function reset(): void {
  form.displayName = '';
  form.username = '';
  form.password = '';
  form.passwordConfirmation = '';
  errors.displayName = '';
  errors.username = '';
  errors.password = '';
  errors.passwordConfirmation = '';
  formError.value = '';
}

function openCreate(): void {
  reset();
  formOpen.value = true;
}

function closeForm(): void {
  if (saving.value) return;
  formOpen.value = false;
  // Always clear the form — and therefore both password fields — on close so no
  // secret lingers in memory once the modal is dismissed.
  reset();
}

function validate(): boolean {
  errors.displayName = validateRequired(form.displayName, 'Nom affiché');

  const username = form.username.trim();
  if (!username) {
    errors.username = 'Nom d’utilisateur est obligatoire.';
  } else if (username.length < 3 || username.length > 80) {
    errors.username = 'Le nom d’utilisateur doit contenir entre 3 et 80 caractères.';
  } else if (!USERNAME_PATTERN.test(username)) {
    errors.username = 'Lettres, chiffres, points, tirets et underscores uniquement.';
  } else {
    errors.username = '';
  }

  if (form.password.length < 8 || form.password.length > 72) {
    errors.password = 'Le mot de passe doit contenir entre 8 et 72 caractères.';
  } else {
    errors.password = '';
  }

  errors.passwordConfirmation =
    form.passwordConfirmation === form.password ? '' : 'Les mots de passe ne correspondent pas.';

  return !errors.displayName && !errors.username && !errors.password && !errors.passwordConfirmation;
}

async function save(): Promise<void> {
  formError.value = '';
  if (!validate() || saving.value) return;
  saving.value = true;
  try {
    // Payload carries only the three backend fields — never role, active or the
    // frontend-only password confirmation.
    await store.create({
      displayName: form.displayName.trim(),
      username: form.username.trim(),
      password: form.password,
    });
    formOpen.value = false;
    reset();
    showSuccessToast('Le compte Barmaker a été créé.');
  } catch (err) {
    // Keep the modal open and preserve the typed input; surface field/general error.
    if (isConflict(err)) errors.username = 'Ce nom d’utilisateur est déjà utilisé.';
    else formError.value = describeAdminError(err);
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <section class="stack users-page">
    <BarmakerPageHeader
      eyebrow="GESTION"
      title="Équipe"
      description="Consultez le personnel du bar et créez des comptes d’accès Barmaker."
      action-label="Ajouter un barmaker"
      action-icon="plus"
      @action="openCreate" />

    <SuccessToast :message="toastMessage" :visible="toastVisible" :toast-key="toastId" />
    <p v-if="formError" class="alert error" role="alert">{{ formError }}</p>

    <section v-if="store.loading && !store.loaded" class="card empty-state" aria-busy="true">
      <h2>Chargement de l’équipe…</h2>
    </section>
    <section v-else-if="store.error && !store.items.length" class="card empty-state">
      <h2>Impossible de charger l’équipe</h2>
      <p>{{ store.error }}</p>
      <button class="button" type="button" @click="store.load({ initial: true })">Réessayer</button>
    </section>

    <template v-else>
      <p v-if="store.error" class="alert warning" role="status">{{ store.error }}</p>
      <div v-if="store.items.length" class="users-grid">
        <article v-for="member in store.items" :key="member.id" class="user-card">
          <div class="card-heading">
            <span class="user-mark" aria-hidden="true"><AppIcon name="users" :size="20" /></span>
            <StatusBadge
              :label="member.active ? 'Actif' : 'Inactif'"
              :tone="member.active ? 'success' : 'danger'" />
          </div>
          <div class="user-copy">
            <p class="eyebrow">{{ roleLabel(member.role) }}</p>
            <h2>{{ member.displayName }}</h2>
            <p class="username">@{{ member.username }}</p>
          </div>
        </article>
      </div>
      <section v-else class="card empty-state">
        <h2>Aucun compte</h2>
        <p>Ajoutez un premier barmaker pour lui donner accès à l’espace.</p>
      </section>
    </template>

    <BarmakerFormModal
      :open="formOpen"
      eyebrow="CRÉATION"
      title="Ajouter un barmaker"
      size="compact"
      close-label="Fermer le formulaire barmaker"
      :close-disabled="saving"
      @close="closeForm">
      <form id="barmaker-form" class="users-form" @submit.prevent="save">
        <p v-if="formError" class="alert error" role="alert">{{ formError }}</p>
        <label>Nom affiché <span aria-hidden="true">*</span>
          <input v-model="form.displayName" type="text" autocomplete="name" />
          <span v-if="errors.displayName" class="field-error">{{ errors.displayName }}</span>
        </label>
        <label>Nom d’utilisateur <span aria-hidden="true">*</span>
          <input v-model="form.username" type="text" autocomplete="username" />
          <span v-if="errors.username" class="field-error">{{ errors.username }}</span>
        </label>
        <label>Mot de passe <span aria-hidden="true">*</span>
          <input v-model="form.password" type="password" autocomplete="new-password" />
          <span v-if="errors.password" class="field-error">{{ errors.password }}</span>
        </label>
        <label>Confirmer le mot de passe <span aria-hidden="true">*</span>
          <input v-model="form.passwordConfirmation" type="password" autocomplete="new-password" />
          <span v-if="errors.passwordConfirmation" class="field-error">{{ errors.passwordConfirmation }}</span>
        </label>
        <p class="role-hint">Rôle attribué : Barmaker</p>
      </form>
      <template #footer>
        <button class="button secondary" type="button" :disabled="saving" @click="closeForm">Annuler</button>
        <button class="button" type="submit" form="barmaker-form" :disabled="saving">
          {{ saving ? 'Création…' : 'Créer le compte' }}
        </button>
      </template>
    </BarmakerFormModal>
  </section>
</template>

<style scoped>
.users-page { gap: 30px; }
.users-grid { display: grid; gap: var(--space-5); grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); }
.user-card { position: relative; display: grid; gap: var(--space-4); padding: 22px; border: 1px solid rgba(229, 219, 204, 0.9); border-radius: 22px; background: var(--color-surface); box-shadow: var(--shadow-card-soft); overflow: hidden; }
.user-card::before { content: ""; position: absolute; inset: 0 0 auto; height: 3px; background: var(--color-accent); opacity: 0.76; }
.card-heading { display: flex; justify-content: space-between; gap: var(--space-3); align-items: center; }
.user-mark { width: 44px; height: 44px; display: grid; place-items: center; border-radius: 14px; background: var(--color-surface-muted); color: var(--color-primary); }
.user-copy { display: grid; gap: var(--space-2); }
.user-copy h2 { margin: 0; font-size: 1.28rem; letter-spacing: -0.025em; }
.user-copy .username { margin: 0; color: var(--color-text-secondary); font-weight: 700; }
.users-form { display: grid; gap: var(--space-4); padding: var(--space-1) 0; }
.users-form input { min-height: 50px; border-radius: 14px; }
.role-hint { margin: 0; padding: var(--space-3) var(--space-4); border: 1px dashed rgba(229, 219, 204, 0.9); border-radius: 14px; background: var(--color-surface-muted); color: var(--color-text-secondary); font-weight: 800; }
</style>
