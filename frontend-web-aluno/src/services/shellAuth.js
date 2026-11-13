export function getShellConfig() {
  if (typeof window === 'undefined') {
    return {}
  }

  return window.UNISYNC_SHELL || {}
}

export function getAccessToken() {
  return getShellConfig().accessToken || ''
}

export function logout() {
  const logoutUrl = getShellConfig().logoutUrl

  if (logoutUrl) {
    window.location.assign(logoutUrl)
  }
}