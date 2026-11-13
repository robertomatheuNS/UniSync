<script setup>
defineProps({
  task: { type: Object, required: true },
  previousColumnId: { type: String, default: null },
  nextColumnId: { type: String, default: null },
})

const emit = defineEmits(['move'])
</script>

<template>
  <article class="kanban-task">
    <p class="kanban-task__title">{{ task.title }}</p>

    <div class="kanban-task__actions">
      <button
        v-if="previousColumnId"
        type="button"
        class="kanban-task__button"
        :aria-label="`Mover ${task.title} para a coluna anterior`"
        @click="emit('move', previousColumnId)"
      >
        ←
      </button>
      <button
        v-if="nextColumnId"
        type="button"
        class="kanban-task__button"
        :aria-label="`Mover ${task.title} para a próxima coluna`"
        @click="emit('move', nextColumnId)"
      >
        →
      </button>
    </div>
  </article>
</template>

<style scoped>
.kanban-task {
  padding: var(--space-sm);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  border-radius: var(--radius-md);
  background: var(--color-task);
}

.kanban-task__title {
  flex: 1;
}

.kanban-task__actions {
  display: flex;
  gap: 0.25rem;
}

.kanban-task__button {
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: 50%;
  background: var(--color-sidebar);
}
</style>