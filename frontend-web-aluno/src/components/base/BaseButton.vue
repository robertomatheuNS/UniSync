<script setup>
defineProps({
  type: { type: String, default: 'button' },
})
</script>

<template>
  <button :type="type" class="base-button">
    <slot></slot>
  </button>
</template>

<style scoped>
.base-button {
  padding: 0.6rem 2.5rem;
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: var(--color-on-primary);
}
</style>
