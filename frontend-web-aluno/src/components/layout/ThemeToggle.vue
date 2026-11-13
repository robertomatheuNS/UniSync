<script setup>
import { useTheme } from '@/composables/useTheme'

const { isDark, toggleTheme } = useTheme()
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-checked="isDark"
    aria-label="Tema escuro"
    class="theme-toggle"
    @click="toggleTheme"
  >
    <span
      class="theme-toggle__thumb"
      :class="{ 'theme-toggle__thumb--light': !isDark }"
      aria-hidden="true"
    />
    <span
      class="theme-toggle__icon"
      :class="{ 'theme-toggle__icon--on-thumb': isDark }"
      aria-hidden="true"
    >☾</span>
    <span
      class="theme-toggle__icon"
      :class="{ 'theme-toggle__icon--on-thumb': !isDark }"
      aria-hidden="true"
    >☀</span>
  </button>
</template>

<style scoped>
.theme-toggle {
  position: relative;
  width: 4.5rem;
  height: 2rem;
  padding: 0.2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-toggle-track);
  color: var(--color-white);
}

.theme-toggle__thumb {
  position: absolute;
  top: 0.2rem;
  left: 0.2rem;
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 50%;
  background: var(--color-toggle-thumb);
  transition: transform 0.2s;
}

.theme-toggle__thumb--light {
  transform: translateX(2.5rem);
}

.theme-toggle__icon {
  position: relative;
  width: 1.6rem;
  text-align: center;
}

/* O símbolo que está sobre a bolinha clara precisa ser escuro */
.theme-toggle__icon--on-thumb {
  color: var(--color-toggle-track);
}

@media (prefers-reduced-motion: reduce) {
  .theme-toggle__thumb {
    transition: none;
  }
}
</style>