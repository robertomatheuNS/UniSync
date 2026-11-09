from fastapi import FastAPI
from fastapi.testclient import TestClient

from src.routers.oportunidades import router


def test_atualizacao_sem_campos():
    app = FastAPI()
    app.include_router(router)

    with TestClient(app) as client:
        resposta = client.patch(
            "/oportunidades/atualizar/507f1f77bcf86cd799439011",
            json={},
        )

    assert resposta.status_code == 400
    assert resposta.json()["detail"] == "Nenhum campo foi informado"
    