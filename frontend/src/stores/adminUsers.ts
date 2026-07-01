import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { createBarmaker, fetchUsers } from '@/services/usersApi';
import type { CreateBarmakerRequest, UserAdminResponse } from '@/types/api';
import { describeAdminError } from '@/utils/adminErrors';

/**
 * Manager-only staff administration store. Loads every staff account and creates
 * new barmakers. Guards against duplicate concurrent loads and stale responses,
 * and deterministically refreshes the list after a successful creation. The list
 * is never mutated locally before the API confirms. No password is ever cached.
 */
export const useAdminUsersStore = defineStore('adminUsers', () => {
  const items = ref<UserAdminResponse[]>([]);
  const loading = ref(false);
  const error = ref('');
  const loaded = ref(false);

  let inFlight = false;
  let listSeq = 0;

  const hasItems = computed(() => items.value.length > 0);

  function sortInPlace(list: UserAdminResponse[]): UserAdminResponse[] {
    return [...list].sort(
      (a, b) =>
        a.displayName.localeCompare(b.displayName, 'fr') ||
        a.username.localeCompare(b.username, 'fr') ||
        a.id - b.id,
    );
  }

  async function load(options: { initial?: boolean } = {}): Promise<void> {
    if (inFlight) return;
    inFlight = true;
    const seq = ++listSeq;
    if (options.initial && !loaded.value) loading.value = true;
    try {
      const data = await fetchUsers();
      if (seq !== listSeq) return;
      items.value = sortInPlace(data);
      loaded.value = true;
      error.value = '';
    } catch (err) {
      if (seq === listSeq) error.value = describeAdminError(err);
    } finally {
      inFlight = false;
      if (seq === listSeq) loading.value = false;
    }
  }

  /** Create a barmaker; on success the list is refreshed. Throws on failure. */
  async function create(payload: CreateBarmakerRequest): Promise<UserAdminResponse> {
    const created = await createBarmaker(payload);
    await load();
    return created;
  }

  return {
    items,
    loading,
    error,
    loaded,
    hasItems,
    load,
    create,
  };
});
