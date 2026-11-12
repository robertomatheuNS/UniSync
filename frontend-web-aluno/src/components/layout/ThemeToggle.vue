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
    <span class="theme-toggle__icon" aria-hidden="true">☾</span>
    <span class="theme-toggle__icon" aria-hidden="true">☀</span>
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
  background: var(--color-card);
  color: var(--color-text);
}

.theme-toggle__thumb {
  position: absolute;
  top: 0.2rem;
  left: 0.2rem;
  width: 1.6rem;
  height: 1.6rem;
  border-radius: 50%;
  background: var(--color-field);
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

@media (prefers-reduced-motion: reduce) {
  .theme-toggle__thumb {
    transition: none;
  }
}
</style>