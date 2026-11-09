from django.test import TestCase
from django.urls import reverse


class AuthenticationTests(TestCase):
    def test_login_page_loads(self):
        response = self.client.get(reverse("login"))

        self.assertEqual(response.status_code, 200)

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

    def test_login_with_valid_credentials(self):
        response = self.client.post(
            reverse("login"),
            {
                "email": "teste@unisync.com",
                "senha": "123456",
            },
        )

        self.assertRedirects(response, reverse("dashboard"))
        self.assertIn("auth_token", self.client.session)

    def test_dashboard_requires_authentication(self):
        response = self.client.get(reverse("dashboard"))

        self.assertRedirects(response, reverse("login"))

    def test_authenticated_user_can_access_dashboard(self):
        self.client.login()

        session = self.client.session
        session["auth_token"] = "token-de-desenvolvimento"
        session.save()

        response = self.client.get(reverse("dashboard"))

        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "Você está autenticado.")

    def test_logout_clears_session(self):
        session = self.client.session
        session["auth_token"] = "token-de-desenvolvimento"
        session.save()

        response = self.client.get(reverse("logout"))

        self.assertRedirects(response, reverse("login"))
        self.assertNotIn("auth_token", self.client.session)