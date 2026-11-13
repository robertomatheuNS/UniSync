import { api } from './api'
export const listarLembretes = () => api('/lembretes/listar')
export const criarLembrete = (titulo) =>
  api('/lembretes/criar', { method: 'POST', body: JSON.stringify({ titulo }) })
export const atualizarLembrete = (id, dados) =>
  api('/lembretes/atualizar/' + encodeURIComponent(id), {
    method: 'PATCH',
    body: JSON.stringify(dados),
  })
export const excluirLembrete = (id) =>
  api('/lembretes/excluir/' + encodeURIComponent(id), { method: 'DELETE' })
