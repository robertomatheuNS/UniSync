<script setup>
import { ref } from 'vue'
import KanbanTask from '@/components/kanban/KanbanTask.vue'

const props = defineProps({
  column: { type: Object, required: true },
  tasks: { type: Array, required: true },
})

const emit = defineEmits(['move', 'remove'])

const isDragOver = ref(false)

function handleDragLeave(event) {
  // Ignora quando o mouse só passou para um elemento dentro da própria coluna
  if (event.currentTarget.contains(event.relatedTarget)) return
  isDragOver.value = false
}

function handleDrop(event) {
  isDragOver.value = false

  const taskId = event.dataTransfer.getData('text/plain')
  if (taskId) emit('move', taskId, props.column.id)
}
</script>

<template>
  <section
    class="kanban-column"
    :class="{ 'kanban-column--drag-over': isDragOver }"
    :aria-labelledby="`column-${column.id}`"
    @dragover.prevent="isDragOver = true"
    @dragleave="handleDragLeave"
    @drop.prevent="handleDrop"
  >
    <h3 :id="`column-${column.id}`" class="kanban-column__title">{{ column.title }}</h3>

    <ul class="kanban-column__list">
      <KanbanTask
        v-for="task in tasks"
        :key="task.id"
        :task="task"
        @remove="emit('remove', task.id)"
      />
    </ul>
  </section>
</template>

<style scoped>
.kanban-column {
  min-height: 20rem;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-card);
  outline: 2px dashed transparent;
  outline-offset: -4px;
  transition: outline-color 0.2s;
}

.kanban-column--drag-over {
  outline-color: var(--color-text-muted);
}

.kanban-column__title {
  margin-bottom: var(--space-md);
  color: var(--color-text-muted);
  font-size: 1.5rem;
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