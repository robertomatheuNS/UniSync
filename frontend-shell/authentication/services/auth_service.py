from dataclasses import dataclass

import requests
from django.conf import settings


@dataclass
class AuthenticationResult:
    success: bool
    token: str | None = None
    message: str | None = None


class AuthenticationService:
    def login(self, email: str, senha: str) -> AuthenticationResult:
        if settings.AUTH_MOCK:
            return self._login_mock(email, senha)

        return self._login_api(email, senha)

    def _login_mock(self, email: str, senha: str) -> AuthenticationResult:
        if email == "teste@unisync.com" and senha == "123456":
            return AuthenticationResult(
                success=True,
                token="token-de-desenvolvimento",
            )

        return AuthenticationResult(
            success=False,
            message="E-mail ou senha inválidos.",
        )

    def _login_api(self, email: str, senha: str) -> AuthenticationResult:
        try:
            response = requests.post(
                f"{settings.API_BASE_URL}/api/usuarios/login",
                json={
                    "email": email,
                    "senha": senha,
                },
                timeout=5,
            )
        except requests.RequestException:
            return AuthenticationResult(
                success=False,
                message="Não foi possível conectar ao serviço de autenticação.",
            )

        if response.status_code == 200:
            dados = response.json()

            return AuthenticationResult(
                success=True,
                token=dados.get("token"),
            )

        if response.status_code == 401:
            return AuthenticationResult(
                success=False,
                message="E-mail ou senha inválidos.",
            )

        return AuthenticationResult(
            success=False,
            message="Ocorreu um erro ao realizar a autenticação.",
        )