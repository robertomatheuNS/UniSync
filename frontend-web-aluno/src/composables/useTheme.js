import { computed, ref, watch } from 'vue'

const STORAGE_KEY = 'unisync-theme'
const LIGHT = 'light'
const DARK = 'dark'

function getInitialTheme() {
  const savedTheme = localStorage.getItem(STORAGE_KEY)
  if (savedTheme === LIGHT || savedTheme === DARK) return savedTheme

  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  return prefersDark ? DARK : LIGHT
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme
  localStorage.setItem(STORAGE_KEY, theme)
}

const theme = ref(getInitialTheme())

export function initTheme() {
  watch(theme, applyTheme, { immediate: true })
}

export function useTheme() {
  const isDark = computed(() => theme.value === DARK)

  function toggleTheme() {
    theme.value = isDark.value ? LIGHT : DARK
  }

  return { isDark, toggleTheme }
}