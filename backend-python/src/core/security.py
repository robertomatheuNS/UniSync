
import base64
from dataclasses import dataclass
from uuid import UUID
import jwt
from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from src.core.config import settings


CLAIM_ID = (
    "http://schemas.xmlsoap.org/ws/2005/05/"
    "identity/claims/nameidentifier"
)

CLAIM_NAME = (
    "http://schemas.xmlsoap.org/ws/2005/05/"
    "identity/claims/name"
)

CLAIM_ROLE = (
    "http://schemas.microsoft.com/ws/2008/06/"
    "identity/claims/role"
)


@dataclass(frozen=True)
class UsuarioAutenticado:
    id: UUID
    nome: str
    papel: str


def credenciais_invalidas():
    return HTTPException(
        status_code=401,
        detail="Token inválido ou expirado",
        headers={"WWW-Authenticate": "Bearer"},
    )


def validar_token(token: str) -> UsuarioAutenticado:

    try:
        chave = base64.b64decode(
        settings.jwt_key,
        validate=True,
    )

        payload = jwt.decode(
            token,
            key=chave,
            algorithms=["HS256"],
            issuer=settings.jwt_issuer,
            audience=settings.jwt_audience,
            options={
                "require": ["exp", "iss", "aud"],
            },
        )

        def obter_claim(nome_completo: str, nome_curto: str):
            valor = payload.get(nome_completo)

            if valor is None:
                valor = payload.get(nome_curto)

            if valor is None:
                raise ValueError(f"Claim obrigatória ausente: {nome_curto}")

            return valor


        usuario_id = UUID(
            obter_claim(CLAIM_ID, "nameid")
        )

        nome = obter_claim(CLAIM_NAME, "unique_name")

        papel = obter_claim(CLAIM_ROLE, "role")

        if not isinstance(nome, str) or not nome.strip():
            raise ValueError("Nome inválido")

        if papel not in ("Professor", "Aluno"):
            raise ValueError("Papel inválido")

        return UsuarioAutenticado(
            id=usuario_id,
            nome=nome,
            papel=papel,
        )

    except (
        jwt.InvalidTokenError,
        KeyError,
        ValueError,
        TypeError,
        base64.binascii.Error,
    ) as erro:
        raise credenciais_invalidas() from erro


bearer_scheme = HTTPBearer(auto_error=False)


def obter_usuario_atual(
    credenciais: HTTPAuthorizationCredentials | None = Depends(
        bearer_scheme
    ),
) -> UsuarioAutenticado:
    if credenciais is None:
        raise HTTPException(
            status_code=401,
            detail="Token de autenticação não informado",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return validar_token(credenciais.credentials)


def exigir_professor(
    usuario: UsuarioAutenticado = Depends(obter_usuario_atual),
) -> UsuarioAutenticado:

    if usuario.papel != "Professor":
        raise HTTPException(
            status_code=403,
            detail="Acesso permitido apenas para professores",
        )

    return usuario
