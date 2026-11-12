const NOTICE_TYPES = ['Atividade', 'Palestra', 'Evento', 'Prova', 'Trabalho']

export const NOTICE_TYPE_OPTIONS = NOTICE_TYPES.map((type) => ({
  value: type,
  label: type,
}))