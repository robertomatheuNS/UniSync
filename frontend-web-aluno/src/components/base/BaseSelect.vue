<script setup>
defineProps({
  id: { type: String, required: true },
  label: { type: String, required: true },
  options: { type: Array, required: true },
  error: { type: String, default: '' },
})

const model = defineModel({ type: String, default: '' })
</script>

<template>
  <div class="base-select">
    <label :for="id" class="base-select__label">{{ label }}</label>
    <select
      :id="id"
      v-model="model"
      class="base-select__field"
      :aria-invalid="Boolean(error)"
    >
      <option value="" disabled>Selecione</option>
      <option v-for="option in options" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
    <span v-if="error" class="base-select__error" role="alert">{{ error }}</span>
  </div>
</template>

<style scoped>
.base-select {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.base-select__field {
  padding: 0.6rem;
  border: 1px solid transparent;
  background: var(--color-field);
  font: inherit;
}

.base-select__field[aria-invalid='true'] {
  border-color: var(--color-error);
}

.base-select__error {
  color: var(--color-error);
  font-size: 0.85rem;
}
</style>