import { onMounted, onUnmounted } from 'vue';

/**
 * Reliable interval polling for a Vue component:
 *  - a single interval, started on mount and cleared on unmount (no duplicates
 *    after repeated navigation);
 *  - ticks are skipped while the document is hidden, and an immediate refresh
 *    runs as soon as the tab becomes visible again;
 *  - overlap between consecutive HTTP calls is the task's responsibility (the
 *    stores guard against concurrent requests).
 */
export function usePolling(task: () => void | Promise<void>, intervalMs: number) {
  let timer: ReturnType<typeof setInterval> | null = null;

  function tick(): void {
    if (typeof document !== 'undefined' && document.hidden) return;
    void task();
  }

  function start(): void {
    if (timer !== null) return;
    timer = setInterval(tick, intervalMs);
  }

  function stop(): void {
    if (timer !== null) {
      clearInterval(timer);
      timer = null;
    }
  }

  function onVisibility(): void {
    if (typeof document !== 'undefined' && !document.hidden) void task();
  }

  onMounted(() => {
    start();
    document.addEventListener('visibilitychange', onVisibility);
  });

  onUnmounted(() => {
    stop();
    document.removeEventListener('visibilitychange', onVisibility);
  });

  return { start, stop };
}
