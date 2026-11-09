
import os
import uuid
import httpx
import pytest


BASE_URL = os.getenv("API_TEST_URL", "http://127.0.0.1:8000")


@pytest.mark.e2e
def test_crud_com_mongodb_real():
    titulo = f"Teste Integracao {uuid.uuid4().hex[:8]}"
    oportunidade_id = None

    dados = {
        "titulo": titulo,
        "professor": "Professor Teste",
        "materia": "Programação",
        "tipo": "TESTE_INTEGRACAO",
        "data_evento": "2026-12-20T14:00:00",
    }

    with httpx.Client(base_url=BASE_URL, timeout=10.0) as client:
        try:

            resposta = client.get("/health")
            assert resposta.status_code == 200

            resposta = client.post("/oportunidades/criar", json=dados)
            assert resposta.status_code == 201, resposta.text

            oportunidade_id = resposta.json()["id"]

            resposta = client.get(
                f"/oportunidades/buscar/{oportunidade_id}"
            )
            assert resposta.status_code == 200
            assert resposta.json()["titulo"] == titulo

            resposta = client.get(
                "/oportunidades/listar",
                params={"tipo": "TESTE_INTEGRACAO"}
            )
            assert resposta.status_code == 200
            assert any(
                item["id"] == oportunidade_id
                for item in resposta.json()
            )

            resposta = client.patch(
                f"/oportunidades/atualizar/{oportunidade_id}",
                json={"titulo": "Titulo Atualizado"}
            )
            assert resposta.status_code == 200
            assert resposta.json()["titulo"] == "Titulo Atualizado"

            resposta = client.delete(
                f"/oportunidades/excluir/{oportunidade_id}"
            )
            assert resposta.status_code == 204

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
