export function authUrl(caminho) {
  const origem = import.meta.env.VITE_SHELL_URL || 'http://localhost:8081'
  return new URL(caminho, origem).href
}
