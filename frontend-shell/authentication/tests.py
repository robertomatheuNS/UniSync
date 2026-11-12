from unittest.mock import Mock, patch

import requests
from django.test import TestCase, override_settings
from django.urls import reverse

from .services.auth_service import AuthenticationService


class AuthenticationTests(TestCase):
    def test_login_page_loads(self):
        response = self.client.get(reverse("login"))

        self.assertEqual(response.status_code, 200)

    @override_settings(AUTH_MOCK=True)
    def test_login_with_invalid_credentials(self):
        response = self.client.post(
            reverse("login"),
            {
                "email": "usuario@teste.com",
                "senha": "senha-invalida",
            },
        )

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "E-mail ou senha inválidos.")

    @override_settings(AUTH_MOCK=True)
    def test_login_with_valid_credentials(self):
        response = self.client.post(
            reverse("login"),
            {
                "email": "teste@unisync.com",
                "senha": "123456",
            },
        )

        self.assertRedirects(response, reverse("app"))
        self.assertIn("auth_token", self.client.session)

    def test_dashboard_requires_authentication(self):
        response = self.client.get(reverse("dashboard"))

        self.assertRedirects(response, reverse("login"))

    def test_authenticated_user_can_access_app(self):
        session = self.client.session
        session["auth_token"] = "token-de-desenvolvimento"
        session.save()

        response = self.client.get(reverse("app"))

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, 'id="app"')

    def test_logout_clears_session(self):
        session = self.client.session
        session["auth_token"] = "token-de-desenvolvimento"
        session.save()

        response = self.client.get(reverse("logout"))

        self.assertRedirects(response, reverse("login"))
        self.assertNotIn("auth_token", self.client.session)

    @override_settings(
        AUTH_MOCK=False,
        API_BASE_URL="http://localhost:5040",
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
        API_BASE_URL="http://localhost:5040",
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
        API_BASE_URL="http://localhost:5040",
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

    @override_settings(AUTH_MOCK=True)
    def test_register_mock_success(self):
        resultado = AuthenticationService().register(
            nome="Usuário de Teste",
            email="novo@unisync.com",
            senha="12345678",
        )

        self.assertTrue(resultado.success)
        self.assertEqual(resultado.user["nome"], "Usuário de Teste")
        self.assertEqual(resultado.user["email"], "novo@unisync.com")

    @override_settings(AUTH_MOCK=True)
    def test_register_mock_existing_email(self):
        resultado = AuthenticationService().register(
            nome="Usuário Existente",
            email="cadastrado@unisync.com",
            senha="12345678",
        )

        self.assertFalse(resultado.success)
        self.assertEqual(
            resultado.message,
            "O e-mail informado já está cadastrado.",
        )

    @override_settings(
        AUTH_MOCK=False,
        API_BASE_URL="http://localhost:5040",
    )
    @patch("authentication.services.auth_service.requests.post")
    def test_api_register_success(self, mock_post):
        response_api = Mock()
        response_api.status_code = 201
        response_api.json.return_value = {
            "id": "usuario-de-teste",
            "nome": "Usuário de Teste",
            "email": "novo@unisync.com",
            "dataCriacaoUtc": "2026-11-10T00:00:00Z",
        }
        mock_post.return_value = response_api

        resultado = AuthenticationService().register(
            nome="Usuário de Teste",
            email="novo@unisync.com",
            senha="12345678",
        )

        self.assertTrue(resultado.success)
        self.assertEqual(
            resultado.user["email"],
            "novo@unisync.com",
        )