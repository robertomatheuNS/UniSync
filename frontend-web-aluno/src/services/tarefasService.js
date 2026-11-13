import { api } from './api'
export const listarTarefas = () => api('/tarefas/listar')
export const criarTarefa = (titulo) =>
  api('/tarefas/criar', { method: 'POST', body: JSON.stringify({ titulo }) })
export const atualizarTarefa = (id, dados) =>
  api('/tarefas/atualizar/' + encodeURIComponent(id), {
    method: 'PATCH',
    body: JSON.stringify(dados),
  })
export const excluirTarefa = (id) =>
  api('/tarefas/excluir/' + encodeURIComponent(id), { method: 'DELETE' })
