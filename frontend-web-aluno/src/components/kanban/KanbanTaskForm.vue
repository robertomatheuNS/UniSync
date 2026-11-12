<script setup>
import { ref } from 'vue'
import BaseButton from '@/components/base/BaseButton.vue'

const emit = defineEmits(['add'])

const title = ref('')

function handleSubmit() {
  emit('add', title.value)
  title.value = ''
}
</script>

<template>
  <form class="kanban-form" @submit.prevent="handleSubmit">
    <input
      v-model="title"
      type="text"
      class="kanban-form__input"
      placeholder="Nova tarefa"
      aria-label="Nova tarefa"
    />
    <BaseButton type="submit">Adicionar</BaseButton>
  </form>
</template>

<style scoped>
.kanban-form {
  margin-bottom: var(--space-md);
  display: flex;
  gap: var(--space-sm);
}

.kanban-form__input {
  flex: 1;
  min-width: 0;
  padding: 0.6rem;
  border: none;
  background: var(--color-field);
  font: inherit;
}
</style>