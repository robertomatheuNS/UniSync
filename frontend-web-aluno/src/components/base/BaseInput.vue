<script setup>
defineProps({
  id: { type: String, required: true },
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  error: { type: String, default: '' },
})

const model = defineModel({ type: [String, Number], default: '' })
</script>

<template>
  <div class="base-input">
    <label :for="id" class="base-input__label">{{ label }}</label>
    <input
      :id="id"
      v-model="model"
      :type="type"
      class="base-input__field"
      :aria-invalid="Boolean(error)"
    />
    <span v-if="error" class="base-input__error" role="alert">{{ error }}</span>
  </div>
</template>

<style scoped>
.base-input {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.base-input__field {
  padding: 0.6rem;
  border: 1px solid transparent;
  background: var(--color-field);
  font: inherit;
}

.base-input__field[aria-invalid='true'] {
  border-color: var(--color-error);
}

.base-input__error {
  color: var(--color-error);
  font-size: 0.85rem;
}
</style>