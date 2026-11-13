import { onMounted, onUnmounted, ref } from 'vue'

const DEFAULT_INTERVAL_MS = 5000

export function useCarousel(total, intervalMs = DEFAULT_INTERVAL_MS) {
  const currentIndex = ref(0)
  const isPaused = ref(false)
  let timerId = null

  function next() {
    currentIndex.value = (currentIndex.value + 1) % total
  }

  function stop() {
    clearInterval(timerId)
    timerId = null
  }

  function start() {
    stop()

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReducedMotion || total <= 1) return

    timerId = setInterval(next, intervalMs)
  }

  function goTo(index) {
    currentIndex.value = index
    if (!isPaused.value) start()
  }

  function pause() {
    isPaused.value = true
    stop()
  }

  function resume() {
    isPaused.value = false
    start()
  }

  onMounted(start)
  onUnmounted(stop)

  return { currentIndex, isPaused, goTo, pause, resume }
}