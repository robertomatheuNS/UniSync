import { ref } from 'vue'
import { KANBAN_COLUMNS } from '@/constants/kanban'
import { INITIAL_TASKS } from '@/data/kanbanTasks'

export function useKanban() {
  const tasks = ref([...INITIAL_TASKS])

  function getTasksByColumn(columnId) {
    return tasks.value.filter((task) => task.columnId === columnId)
  }

  function addTask(title) {
    const cleanTitle = title.trim()
    if (!cleanTitle) return

    tasks.value.push({
      id: crypto.randomUUID(),
      title: cleanTitle,
      columnId: KANBAN_COLUMNS[0].id,
    })
  }

  function moveTask(taskId, columnId) {
    const task = tasks.value.find((item) => item.id === taskId)
    if (task) task.columnId = columnId
  }

  return { getTasksByColumn, addTask, moveTask }
}
