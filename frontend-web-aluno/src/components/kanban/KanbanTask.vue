<script setup>
import { ref } from 'vue'
import trashIcon from '@/assets/icons/trash.svg'

const props = defineProps({
  task: { type: Object, required: true },
})

const emit = defineEmits(['remove'])

const isDragging = ref(false)

function handleDragStart(event) {
  event.dataTransfer.setData('text/plain', String(props.task.id))
  event.dataTransfer.effectAllowed = 'move'
  isDragging.value = true
}

function handleDragEnd() {
  isDragging.value = false
}
</script>

<template>
  <li
    class="kanban-task"
    :class="{ 'kanban-task--dragging': isDragging }"
    draggable="true"
    @dragstart="handleDragStart"
    @dragend="handleDragEnd"
  >
    <span class="kanban-task__bullet" aria-hidden="true">•</span>
    <p class="kanban-task__title">{{ task.title }}</p>
    <button
      type="button"
      class="kanban-task__remove"
      :aria-label="`Apagar tarefa ${task.title}`"
      @click="emit('remove')"
    >
      <img :src="trashIcon" alt="" class="kanban-task__remove-icon icon" />
    </button>
  </li>
</template>

<style scoped>
.kanban-task {
  padding: var(--space-sm);
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: end;
  gap: var(--space-sm);
  border-radius: var(--radius-md);
  background: var(--color-task);
  color: var(--color-text);
  cursor: grab;
}

.kanban-task--dragging {
  opacity: 0.4;
}

.kanban-task__bullet {
  align-self: start;
  font-size: 1.4rem;
  line-height: 1.2;
}

.kanban-task__title {
  font-size: 1.4rem;
  line-height: 1.2;
}

.kanban-task__remove {
  padding: 0.15rem;
  border: none;
  background: none;
}

.kanban-task__remove-icon {
  width: 1.1rem;
  height: 1.1rem;
  opacity: 0.6;
}
</style>