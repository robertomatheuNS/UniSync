import { obterSessao } from '@/utils/sessao'
const base = (
  import.meta.env.VITE_PYTHON_API_URL || 'http://localhost:8000'
).replace(/\/$/, '')
export async function api(caminho, opcoes = {}) {
  const sessao = obterSessao()
  if (!sessao) throw new Error('Sua sessão expirou. Entre novamente.')
  let resposta
  try {
    resposta = await fetch(base + caminho, {
      ...opcoes,
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + sessao.token,
      },
    })
  } catch {
    throw new Error('Não foi possível conectar ao servidor. Tente novamente.')
  }
  if (!resposta.ok) {
    const mensagens = {
      401: 'Sua sessão expirou. Entre novamente.',
      403: 'Sua conta não tem permissão para esta ação.',
      404: 'O item já foi removido. Atualize a lista.',
      422: 'Confira os dados informados.',
    }
    throw new Error(
      mensagens[resposta.status] || 'Não foi possível salvar. Tente novamente.',
    )
  }
  return resposta.status === 204 ? null : resposta.json()
}
