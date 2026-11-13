<script setup>
import KanbanTask from '@/components/kanban/kanbanTask.vue'

defineProps({
  column: { type: Object, required: true },
  tasks: { type: Array, required: true },
  previousColumnId: { type: String, default: null },
  nextColumnId: { type: String, default: null },
})

const emit = defineEmits(['move'])
</script>

<template>
  <section class="kanban-column" :aria-labelledby="`column-${column.id}`">
    <h3 :id="`column-${column.id}`" class="kanban-column__title">{{ column.title }}</h3>

    <ul class="kanban-column__list">
      <li v-for="task in tasks" :key="task.id">
        <KanbanTask
          :task="task"
          :previous-column-id="previousColumnId"
          :next-column-id="nextColumnId"
          @move="(columnId) => emit('move', task.id, columnId)"
        />
      </li>
    </ul>
  </section>
</template>

<style scoped>
.kanban-column {
  min-height: 18rem;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-card);
}

.kanban-column__title {
  margin-bottom: var(--space-md);
  color: var(--color-text-muted);
  text-align: center;
  text-transform: uppercase;
}

.kanban-column__list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
</style>