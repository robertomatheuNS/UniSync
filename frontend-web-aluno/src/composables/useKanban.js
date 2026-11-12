
import { ref } from 'vue'

import {
  listarTarefas,
  criarTarefa,
  atualizarTarefa,
} from '@/services/tarefasService'

const STATUS_PARA_COLUNA = {
  A_FAZER: 'todo',
  EM_ANDAMENTO: 'doing',
  CONCLUIDO: 'done',
}

const COLUNA_PARA_STATUS = {
  todo: 'A_FAZER',
  doing: 'EM_ANDAMENTO',
  done: 'CONCLUIDO',
}

function converterTarefa(tarefa) {
  const columnId = STATUS_PARA_COLUNA[tarefa.status]

  if (!columnId) {
    throw new Error(`Status desconhecido: ${tarefa.status}`)
  }

  return {
    id: tarefa.id,
    title: tarefa.titulo,
    columnId,
  }
}

export function useKanban(obterToken) {
  const tasks = ref([])
  const loading = ref(false)
  const error = ref('')

  function tokenAtual() {
    const token = obterToken()

    if (!token) {
      throw new Error('Informe um token de aluno.')
    }

    return token
  }

  function getTasksByColumn(columnId) {
    return tasks.value.filter(
      (task) => task.columnId === columnId
    )
  }

  async function loadTasks() {
    loading.value = true
    error.value = ''

    try {
      const resultado = await listarTarefas(tokenAtual())

      tasks.value = resultado.map(converterTarefa)
    } catch (erro) {
      tasks.value = []
      error.value = erro.message
    } finally {
      loading.value = false
    }
  }

  async function addTask(title) {
    const cleanTitle = title.trim()

    if (!cleanTitle) return

    error.value = ''

    try {
      const tarefa = await criarTarefa(
        cleanTitle,
        tokenAtual()
      )

      tasks.value.push(converterTarefa(tarefa))
    } catch (erro) {
      error.value = erro.message
    }
  }

  async function moveTask(taskId, columnId) {
    const status = COLUNA_PARA_STATUS[columnId]

    if (!status) return

    const task = tasks.value.find(
      (item) => item.id === taskId
    )

    if (!task || task.columnId === columnId) return

    error.value = ''

    try {
      const atualizada = await atualizarTarefa(
        taskId,
        { status },
        tokenAtual()
      )

      task.columnId = converterTarefa(atualizada).columnId
    } catch (erro) {
      error.value = erro.message
    }
  }

  return {
    tasks,
    loading,
    error,
    getTasksByColumn,
    loadTasks,
    addTask,
    moveTask,
  }
}
