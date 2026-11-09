
import base64
from datetime import datetime, timedelta, timezone
from uuid import uuid4
import jwt
import pytest
from fastapi import HTTPException
from src.core import security


def configurar_jwt_teste(monkeypatch):
    chave = b"chave-local-de-teste-com-pelo-menos-32-bytes"

    monkeypatch.setattr(
        security.settings,
        "jwt_key",
        base64.b64encode(chave).decode(),
    )
    monkeypatch.setattr(
        security.settings,
        "jwt_issuer",
        "UniSync.Auth",
    )
    monkeypatch.setattr(
        security.settings,
        "jwt_audience",
        "UniSync.Services",
    )

    return chave


def criar_token(chave, expiracao):
    return jwt.encode(
        {
            security.CLAIM_ID: str(uuid4()),
            security.CLAIM_NAME: "Professor Teste",
            security.CLAIM_ROLE: "Professor",
            "iss": "UniSync.Auth",
            "aud": "UniSync.Services",
            "exp": expiracao,
        },
        chave,
        algorithm="HS256",
    )


def test_aceitar_token_valido(monkeypatch):
    chave = configurar_jwt_teste(monkeypatch)

    token = criar_token(
        chave,
        datetime.now(timezone.utc) + timedelta(hours=1),
    )

    usuario = security.validar_token(token)

    assert usuario.nome == "Professor Teste"
    assert usuario.papel == "Professor"


def test_rejeitar_token_expirado(monkeypatch):
    chave = configurar_jwt_teste(monkeypatch)

    token = criar_token(
        chave,
        datetime.now(timezone.utc) - timedelta(hours=1),
    )

    with pytest.raises(HTTPException) as erro:
        security.validar_token(token)

    assert erro.value.status_code == 401


def test_rejeitar_token_invalido(monkeypatch):
    configurar_jwt_teste(monkeypatch)

    with pytest.raises(HTTPException) as erro:
        security.validar_token("token.invalido.aqui")

    assert erro.value.status_code == 401
