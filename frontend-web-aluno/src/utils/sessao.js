// Os claims orientam a interface. A assinatura e as permissões são validadas pela API.
export function obterSessao() {
  try {
    const token = window.UNISYNC_SHELL?.accessToken
    if (!token) return null
    const parte = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(
      atob(parte.padEnd(Math.ceil(parte.length / 4) * 4, '=')),
      (caractere) => caractere.charCodeAt(0),
    )
    const claims = JSON.parse(new TextDecoder().decode(bytes))
    if (!Number.isFinite(claims.exp) || claims.exp * 1000 <= Date.now())
      return null
    return {
      token,
      id:
        claims[
          'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier'
        ] ?? claims.nameid,
      nome:
        claims['http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name'] ??
        claims.unique_name,
      papel:
        claims[
          'http://schemas.microsoft.com/ws/2008/06/identity/claims/role'
        ] ?? claims.role,
    }
  } catch {
    return null
  }
}
