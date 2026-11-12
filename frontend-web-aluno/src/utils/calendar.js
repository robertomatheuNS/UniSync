/**
 * Devolve uma lista com os dias do mês de uma data.
 * Antes do dia 1, coloca `null` para alinhar com o dia da semana.
 */
export function buildMonthCells(date) {
  const year = date.getFullYear()
  const month = date.getMonth()

  const firstWeekday = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()

  const emptyCells = Array(firstWeekday).fill(null)
  const days = Array.from({ length: daysInMonth }, (_, index) => index + 1)

  return [...emptyCells, ...days]
}