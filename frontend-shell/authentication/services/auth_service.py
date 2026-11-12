from dataclasses import dataclass

import requests
from django.conf import settings


@dataclass
class AuthenticationResult:
    success: bool
    token: str | None = None
    message: str | None = None


@dataclass
class RegistrationResult:
    success: bool
    user: dict[str, str] | None = None
    message: str | None = None


class AuthenticationService:
    def login(self, email: str, senha: str) -> AuthenticationResult:
        if settings.AUTH_MOCK:
            return self._login_mock(email, senha)

        return self._login_api(email, senha)

    def register(
        self,
        nome: str,
        email: str,
        senha: str,
        tipo_usuario: str = "aluno",
        codigo_professor: str | None = None,
    ) -> RegistrationResult:
        if settings.AUTH_MOCK:
            return self._register_mock(
                nome,
                email,
                senha,
                tipo_usuario,
                codigo_professor,
            )

        return self._register_api(
            nome,
            email,
            senha,
            tipo_usuario,
            codigo_professor,
        )

    def _login_mock(
        self,
        email: str,
        senha: str,
    ) -> AuthenticationResult:
        if email == "teste@unisync.com" and senha == "123456":
            return AuthenticationResult(
                success=True,
                token="token-de-desenvolvimento",
            )

        return AuthenticationResult(
            success=False,
            message="E-mail ou senha inválidos.",
        )

    def _login_api(
        self,
        email: str,
        senha: str,
    ) -> AuthenticationResult:
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

    def _register_mock(
        self,
        nome: str,
        email: str,
        senha: str,
        tipo_usuario: str = "aluno",
        codigo_professor: str | None = None,
    ) -> RegistrationResult:
        if email == "cadastrado@unisync.com":
            return RegistrationResult(
                success=False,
                message="O e-mail informado já está cadastrado.",
            )

        if tipo_usuario == "professor" and not codigo_professor:
            return RegistrationResult(
                success=False,
                message="O código do professor é obrigatório.",
            )

        return RegistrationResult(
            success=True,
            user={
                "id": "usuario-de-desenvolvimento",
                "nome": nome,
                "email": email,
                "tipo": tipo_usuario,
            },
        )

    def _register_api(
        self,
        nome: str,
        email: str,
        senha: str,
        tipo_usuario: str = "aluno",
        codigo_professor: str | None = None,
    ) -> RegistrationResult:
        if tipo_usuario == "professor":
            endpoint = "/api/usuarios/professores"
            payload = {
                "nome": nome,
                "email": email,
                "senha": senha,
                "codigoProfessor": codigo_professor,
            }
        else:
            endpoint = "/api/usuarios"
            payload = {
                "nome": nome,
                "email": email,
                "senha": senha,
            }

        try:
            response = requests.post(
                f"{settings.API_BASE_URL}{endpoint}",
                json=payload,
                timeout=5,
            )
        except requests.RequestException:
            return RegistrationResult(
                success=False,
                message="Não foi possível conectar ao serviço de autenticação.",
            )

        if response.status_code == 201:
            return RegistrationResult(
                success=True,
                user=response.json(),
            )

        if response.status_code == 409:
            dados = response.json()

            return RegistrationResult(
                success=False,
                message=dados.get(
                    "mensagem",
                    "O e-mail informado já está cadastrado.",
                ),
            )

        if response.status_code == 400:
            dados = response.json()

            return RegistrationResult(
                success=False,
                message=dados.get(
                    "mensagem",
                    "Os dados informados são inválidos.",
                ),
            )

        return RegistrationResult(
            success=False,
            message="Não foi possível realizar o cadastro.",
        )