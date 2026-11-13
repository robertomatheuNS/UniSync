const apiUrl = (
  import.meta.env.VITE_PYTHON_API_URL || 'http://localhost:8000'
).replace(/\/$/, '')

async function requisicao(caminho, { token, ...opcoes } = {}) {
  const resposta = await fetch(`${apiUrl}/oportunidades${caminho}`, {
    ...opcoes,
    headers: {
      ...(opcoes.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  }).catch(() => {
    throw new Error(
      'Não foi possível conectar ao servidor. Confira sua conexão e tente novamente.',
    )
  })
  if (!resposta.ok) {
    const dados = await resposta.json().catch(() => ({}))
    const mensagens = {
      401: 'Sua sessão expirou. Entre novamente para continuar.',
      403: 'Você não tem permissão para alterar esta oportunidade.',
      404: 'Esta oportunidade já foi removida. Atualize a lista.',
      422: 'Confira os campos informados e tente novamente.',
    }
    throw new Error(
      mensagens[resposta.status] ||
        (typeof dados.detail === 'string'
          ? dados.detail
          : 'Não foi possível concluir a operação. Tente novamente.'),
    )
  }
  return resposta.status === 204 ? null : resposta.json()
}

export function listarOportunidades({ tipo = '', futuros = false } = {}) {
  const filtros = new URLSearchParams({
    ordenar: 'true',
    limite: '15',
    futuros: String(futuros),
  })
  if (tipo.trim()) filtros.set('tipo', tipo.trim())
  return requisicao(`/listar?${filtros}`)
}

export const buscarOportunidade = (id) =>
  requisicao(`/buscar/${encodeURIComponent(id)}`)
export const criarOportunidade = (dados, token) =>
  requisicao('/criar', { method: 'POST', body: JSON.stringify(dados), token })
export const atualizarOportunidade = (id, dados, token) =>
  requisicao(`/atualizar/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify(dados),
    token,
  })
export const excluirOportunidade = (id, token) =>
  requisicao(`/excluir/${encodeURIComponent(id)}`, { method: 'DELETE', token })
