export function dataOportunidade(valor) {
  return new Date(/(?:Z|[+-]\d{2}:\d{2})$/i.test(valor) ? valor : `${valor}Z`)
}

export function dataParaFormulario(valor) {
  const data = dataOportunidade(valor)
  const local = new Date(data.getTime() - data.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

export function formatarData(valor) {
  return dataOportunidade(valor).toLocaleString('pt-BR', {
    dateStyle: 'long',
    timeStyle: 'short',
  })
}
