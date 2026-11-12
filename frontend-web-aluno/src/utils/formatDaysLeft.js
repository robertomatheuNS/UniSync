export function formatDaysLeft(days) {
  if (days <= 0) return 'Hoje'
  if (days === 1) return 'Amanhã'
  return `${days} Dias`
}