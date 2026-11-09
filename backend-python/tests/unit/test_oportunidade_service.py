import asyncio

import pytest
from beanie import PydanticObjectId

from src.schemas.oportunidade import OportunidadeUpdate
from src.services.oportunidade_service import atualizar_oportunidade


def test_rejeita_atualizacao_sem_campos():
    oportunidade_id = PydanticObjectId("507f1f77bcf86cd799439011")
    dados_atualizados = OportunidadeUpdate()

    with pytest.raises(ValueError, match="Nenhum campo foi informado"):
        asyncio.run(atualizar_oportunidade(oportunidade_id, dados_atualizados))
