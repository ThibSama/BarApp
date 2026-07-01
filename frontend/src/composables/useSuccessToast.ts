import { onUnmounted, ref } from 'vue';

export function useSuccessToast(durationMs = 3000) {
  const toastMessage = ref('');
  const toastVisible = ref(false);
  const toastId = ref(0);
  let dismissTimer: ReturnType<typeof setTimeout> | undefined;

  function clearDismissTimer(): void {
    if (!dismissTimer) return;
    clearTimeout(dismissTimer);
    dismissTimer = undefined;
  }

  function hideToast(): void {
    clearDismissTimer();
    toastVisible.value = false;
  }

  function showSuccessToast(message: string): void {
    clearDismissTimer();
    toastMessage.value = message;
    toastId.value += 1;
    toastVisible.value = true;
    dismissTimer = setTimeout(() => {
      toastVisible.value = false;
      dismissTimer = undefined;
    }, durationMs);
  }

  onUnmounted(clearDismissTimer);

  return { toastMessage, toastVisible, toastId, showSuccessToast, hideToast };
}
