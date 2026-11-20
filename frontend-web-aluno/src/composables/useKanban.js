import { computed, ref } from 'vue'
import { KANBAN_COLUMNS } from '@/constants/kanban'
import { listarTarefas, criarTarefa, atualizarTarefa, excluirTarefa } from '@/services/tarefasService'

const statusPorColuna = { todo: 'A_FAZER', doing: 'EM_ANDAMENTO', done: 'CONCLUIDO' }
const colunaPorStatus = { A_FAZER: 'todo', EM_ANDAMENTO: 'doing', CONCLUIDO: 'done' }

export function useKanban() {
  const tasks = ref([])
  const loading = ref(false)
  const error = ref('')
  const getTasksByColumn = (columnId) => computed(() => tasks.value.filter((task) => task.columnId === columnId)).value

  function mapear(item) {
    return {
      id: item.id,
      title: item.titulo,
      columnId: colunaPorStatus[item.status] ?? KANBAN_COLUMNS[0].id,
      prioridade: item.prioridade ?? 'MEDIA',
      prazo: item.prazo ?? null,
      turma_id: item.turma_id ?? null,
      disciplina: item.disciplina ?? null,
    }
  }

  async function executar(acao) {
    if (loading.value) return false
    loading.value = true
    error.value = ''
    try {
      await acao()
      return true
    } catch (falha) {
      error.value = falha.message || 'Não foi possível salvar. Tente novamente.'
      return false
    } finally {
      loading.value = false
    }
  }

  const loadTasks = () => executar(async () => { tasks.value = (await listarTarefas()).map(mapear) })
  const addTask = (dados) => executar(async () => {
    const entrada = typeof dados === 'string' ? { titulo: dados } : dados
    const titulo = entrada.titulo?.trim()
    if (!titulo) throw new Error('Informe o título da tarefa.')
    tasks.value.push(mapear(await criarTarefa({ ...entrada, titulo })))
  })
  const editTask = (id, dados) => executar(async () => {
    const entrada = typeof dados === 'string' ? { titulo: dados } : dados
    if (entrada.titulo !== undefined) entrada.titulo = entrada.titulo.trim()
    const atualizado = mapear(await atualizarTarefa(id, entrada))
    tasks.value = tasks.value.map((task) => task.id === id ? atualizado : task)
  })
  const moveTask = (id, columnId) => executar(async () => {
    const status = statusPorColuna[columnId]
    if (!status) return
    const atualizado = mapear(await atualizarTarefa(id, { status }))
    tasks.value = tasks.value.map((task) => task.id === id ? atualizado : task)
  })
  const deleteTask = (id) => executar(async () => {
    await excluirTarefa(id)
    tasks.value = tasks.value.filter((task) => task.id !== id)
  })

  return { tasks, loading, error, loadTasks, addTask, editTask, moveTask, deleteTask, getTasksByColumn }
}
