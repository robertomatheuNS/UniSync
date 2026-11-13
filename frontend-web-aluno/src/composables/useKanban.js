function findTask(taskId) {
  return tasks.value.find((task) => String(task.id) === String(taskId))
}

function addTask(title) {
  const cleanTitle = title.trim()
  if (!cleanTitle) return false

  tasks.value.push({
    id: crypto.randomUUID(),
    title: cleanTitle,
    columnId: KANBAN_COLUMNS[0].id,
  })
  return true
}

function moveTask(taskId, columnId) {
  const task = findTask(taskId)
  if (task) task.columnId = columnId
}

function removeTask(taskId) {
  tasks.value = tasks.value.filter((task) => String(task.id) !== String(taskId))
}

return { getTasksByColumn, addTask, moveTask, removeTask }