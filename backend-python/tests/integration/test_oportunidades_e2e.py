
import os
import uuid
from datetime import datetime, timedelta, timezone
import httpx
import pytest


BASE_URL = os.getenv("API_TEST_URL", "http://127.0.0.1:8000")
DOTNET_AUTH_URL = os.getenv(
    "DOTNET_AUTH_URL",
    "http://host.docker.internal:5041",
)

EMAIL = os.getenv("E2E_PROFESSOR_EMAIL")
SENHA = os.getenv("E2E_PROFESSOR_PASSWORD")

def obter_token_professor():
    if not EMAIL or not SENHA:
        pytest.fail(
            "Configure E2E_PROFESSOR_EMAIL e E2E_PROFESSOR_PASSWORD."
        )

    resposta = httpx.post(
        f"{DOTNET_AUTH_URL}/api/usuarios/login",
        json={
            "email": EMAIL,
            "senha": SENHA,
        },
        timeout=10.0,
    )

    assert resposta.status_code == 200, resposta.text

    token = resposta.json().get("token")
    assert token, "O .NET não retornou um token JWT."

    return token


@pytest.mark.e2e
def test_crud_com_mongodb_real():
    token = obter_token_professor()

    titulo = f"Teste Integracao {uuid.uuid4().hex[:8]}"
    titulo_atualizado = f"{titulo} Atualizado"
    oportunidade_id = None

    data_evento = (
        datetime.now(timezone.utc) + timedelta(days=30)
    ).isoformat()

    dados = {
        "titulo": titulo,
        "professor": "Professor Teste",
        "materia": "Programação",
        "tipo": "TESTE_INTEGRACAO",
        "data_evento": data_evento,
    }

    headers = {
        "Authorization": f"Bearer {token}"
    }

    with httpx.Client(
        base_url=BASE_URL,
        timeout=10.0,
        headers=headers,
    ) as client:
        try:
            resposta = client.get("/health")
            assert resposta.status_code == 200

            resposta = client.post(
                "/oportunidades/criar",
                json=dados,
            )
            assert resposta.status_code == 201, resposta.text

            oportunidade = resposta.json()
            oportunidade_id = oportunidade["id"]

            assert oportunidade["professor_id"] is not None
            assert oportunidade["titulo"] == titulo

            resposta = client.get(
                f"/oportunidades/buscar/{oportunidade_id}"
            )
            assert resposta.status_code == 200
            assert resposta.json()["titulo"] == titulo

            resposta = client.get(
                "/oportunidades/listar",
                params={"tipo": "TESTE_INTEGRACAO"},
            )
            assert resposta.status_code == 200
            assert any(
                item["id"] == oportunidade_id
                for item in resposta.json()
            )

            resposta = client.patch(
                f"/oportunidades/atualizar/{oportunidade_id}",
                json={"titulo": titulo_atualizado},
            )
            assert resposta.status_code == 200, resposta.text
            assert resposta.json()["titulo"] == titulo_atualizado

            resposta = client.get(
                f"/oportunidades/buscar/{oportunidade_id}"
            )
            assert resposta.status_code == 200
            assert resposta.json()["titulo"] == titulo_atualizado

            resposta = client.delete(
                f"/oportunidades/excluir/{oportunidade_id}"
            )
            assert resposta.status_code == 204, resposta.text

            resposta = client.get(
                f"/oportunidades/buscar/{oportunidade_id}"
            )
            assert resposta.status_code == 404

            oportunidade_id = None

        finally:
            if oportunidade_id is not None:
                client.delete(
                    f"/oportunidades/excluir/{oportunidade_id}"
                )
