export function validateNoticeForm(form) {
  const errors = {}

  if (!form.type) {
    errors.type = 'Selecione o tipo do aviso.'
  }
  if (!form.title.trim()) {
    errors.title = 'Informe o título do aviso.'
  }

  const days = Number(form.daysLeft)
  if (form.daysLeft === '' || !Number.isInteger(days) || days < 0) {
    errors.daysLeft = 'Informe os dias restantes (0 ou mais).'
  }

  return errors
}