const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const MIN_PASSWORD_LENGTH = 8

export function validateRegisterForm(form) {
  const errors = {}

  if (!form.name.trim()) {
    errors.name = 'Informe seu nome completo.'
  }
  if (!EMAIL_PATTERN.test(form.email)) {
    errors.email = 'Informe um e-mail válido.'
  }
  if (form.password.length < MIN_PASSWORD_LENGTH) {
    errors.password = `A senha deve ter pelo menos ${MIN_PASSWORD_LENGTH} caracteres.`
  }
  if (!form.course.trim()) {
    errors.course = 'Informe seu curso.'
  }
  if (!form.shift) {
    errors.shift = 'Selecione o turno.'
  }

  return errors
}