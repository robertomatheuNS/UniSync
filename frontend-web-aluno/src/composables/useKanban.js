import { ref } from 'vue'
import {
  listarTarefas,
  criarTarefa,
  atualizarTarefa,
  excluirTarefa,
} from '@/services/tarefasService'
const COLUNAS = { A_FAZER: 'todo', EM_ANDAMENTO: 'doing', CONCLUIDO: 'done' }
const STATUS = { todo: 'A_FAZER', doing: 'EM_ANDAMENTO', done: 'CONCLUIDO' }
const converter = (t) => ({
  id: t.id,
  title: t.titulo,
  columnId: COLUNAS[t.status],
})
export function useKanban() {
  const tasks = ref([]),
    loading = ref(false),
    error = ref('')
  async function executar(acao) {
    if (loading.value) return false
    loading.value = true
    error.value = ''
    try {
      await acao()
      return true
    } catch (e) {
      error.value = e.message
      return false
    } finally {
      loading.value = false
    }
  }
  const loadTasks = () =>
    executar(async () => {
      tasks.value = (await listarTarefas()).map(converter)
    })
  const addTask = (titulo) =>
    executar(async () => {
      tasks.value.push(converter(await criarTarefa(titulo.trim())))
    })
  const editTask = (id, titulo) =>
    executar(async () => {
      const item = converter(
        await atualizarTarefa(id, { titulo: titulo.trim() }),
      )
      tasks.value = tasks.value.map((t) => (t.id === id ? item : t))
    })
  const moveTask = (id, coluna) =>
    executar(async () => {
      if (!STATUS[coluna]) throw new Error('Coluna inválida')
      const item = converter(
        await atualizarTarefa(id, { status: STATUS[coluna] }),
      )
      tasks.value = tasks.value.map((t) => (t.id === id ? item : t))
    })
  const deleteTask = (id) =>
    executar(async () => {
      await excluirTarefa(id)
      tasks.value = tasks.value.filter((t) => t.id !== id)
    })
  return {
    tasks,
    loading,
    error,
    loadTasks,
    addTask,
    editTask,
    moveTask,
    deleteTask,
    getTasksByColumn: (id) => tasks.value.filter((t) => t.columnId === id),
  }
}
