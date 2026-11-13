<script setup>
import { onMounted } from 'vue'

import KanbanColumn from '@/components/kanban/kanbanColumn.vue'
import KanbanTaskForm from '@/components/kanban/KanbanTaskForm.vue'
import { KANBAN_COLUMNS } from '@/constants/kanban'
import { useKanban } from '@/composables/useKanban'
import { getAccessToken } from '@/services/shellAuth'

const {
  loading,
  error,
  getTasksByColumn,
  loadTasks,
  addTask,
  moveTask,
} = useKanban(() => getAccessToken())

onMounted(() => {
  if (getAccessToken()) {
    loadTasks()
  }
})
</script>

<template>
  <section class="kanban">
    <h2 class="kanban__title">Kanban</h2>

    <p v-if="loading">Carregando tarefas...</p>

    <p v-if="error" role="alert">
      {{ error }}
    </p>

    <KanbanTaskForm @add="addTask" />

    <div class="kanban__board">
      <KanbanColumn
        v-for="(column, index) in KANBAN_COLUMNS"
        :key="column.id"
        :column="column"
        :tasks="getTasksByColumn(column.id)"
        :previous-column-id="KANBAN_COLUMNS[index - 1]?.id ?? null"
        :next-column-id="KANBAN_COLUMNS[index + 1]?.id ?? null"
        @move="moveTask"
      />
    </div>
  </section>
</template>

<style scoped>
.kanban {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-sidebar);
}

.kanban__title {
  margin-bottom: var(--space-md);
  color: var(--color-text-muted);
  text-align: center;
  text-transform: uppercase;
}

.kanban__board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
  gap: var(--space-md);
}
</style>