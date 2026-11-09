from unittest.mock import Mock, patch

import requests
from django.test import TestCase, override_settings

from .services.auth_service import AuthenticationService


class AuthenticationTests(TestCase):
    @override_settings(
        AUTH_MOCK=False,
        API_BASE_URL="https://localhost:7129",
    )
    @patch("authentication.services.auth_service.requests.post")
    def test_api_login_success(self, mock_post):
        response_api = Mock()
        response_api.status_code = 200
        response_api.json.return_value = {
            "token": "token-real-de-teste",
        }
        mock_post.return_value = response_api

        resultado = AuthenticationService().login(
            email="usuario@teste.com",
            senha="123456",
        )

        self.assertTrue(resultado.success)
        self.assertEqual(resultado.token, "token-real-de-teste")

    @override_settings(
        AUTH_MOCK=False,
        API_BASE_URL="https://localhost:7129",
    )
    @patch("authentication.services.auth_service.requests.post")
    def test_api_login_invalid_credentials(self, mock_post):
        response_api = Mock()
        response_api.status_code = 401
        mock_post.return_value = response_api

        resultado = AuthenticationService().login(
            email="usuario@teste.com",
            senha="senha-invalida",
        )

        self.assertFalse(resultado.success)
        self.assertEqual(
            resultado.message,
            "E-mail ou senha inválidos.",
        )

    @override_settings(
        AUTH_MOCK=False,
        API_BASE_URL="https://localhost:7129",
    )
    @patch("authentication.services.auth_service.requests.post")
    def test_api_login_connection_error(self, mock_post):
        mock_post.side_effect = requests.RequestException

        resultado = AuthenticationService().login(
            email="usuario@teste.com",
            senha="123456",
        )

        self.assertFalse(resultado.success)
        self.assertEqual(
            resultado.message,
            "Não foi possível conectar ao serviço de autenticação.",
        )