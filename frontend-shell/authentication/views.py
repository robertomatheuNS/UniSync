import base64
import json
import time

from django.contrib import messages
from django.shortcuts import redirect, render
from django.urls import reverse

from .forms import LoginForm, RegisterForm
from .services.auth_service import AuthenticationService


def _jwt_expirado(token):
    """Rejeita um JWT expirado antes de montar o Shell.

    A leitura do payload não substitui a validação criptográfica feita pelas
    APIs. Ela apenas evita renderizar o aplicativo com uma sessão obviamente
    vencida; tokens não-JWT continuam aceitos no modo de desenvolvimento.
    """
    partes = token.split(".")
    if len(partes) != 3:
        return False

    try:
        payload = partes[1] + ("=" * (-len(partes[1]) % 4))
        dados = json.loads(base64.urlsafe_b64decode(payload).decode("utf-8"))
        expiracao = dados.get("exp")
        return not isinstance(expiracao, (int, float)) or expiracao <= time.time()
    except (ValueError, TypeError, UnicodeDecodeError, json.JSONDecodeError):
        return True


def _sessao_valida(request):
    token = request.session.get("auth_token")
    if not token or _jwt_expirado(token):
        if token:
            request.session.flush()
        return False
    return True


def home_view(request):
    return render(request, "authentication/home.html")


def login_view(request):
    if _sessao_valida(request):
        return redirect("app")

    form = LoginForm(request.POST or None)

    if request.method == "POST" and form.is_valid():
        resultado = AuthenticationService().login(
            email=form.cleaned_data["email"],
            senha=form.cleaned_data["senha"],
        )

        if resultado.success and resultado.token:
            request.session.cycle_key()
            request.session["auth_token"] = resultado.token
            return redirect("app")

        form.add_error(
            None,
            resultado.message or "Não foi possível realizar o login.",
        )

    return render(
        request,
        "authentication/login.html",
        {"form": form},
    )


def register_choice_view(request):
    if _sessao_valida(request):
        return redirect("app")

    return render(
        request,
        "authentication/register_choice.html",
    )


def register_view(request, tipo_usuario):
    if _sessao_valida(request):
        return redirect("app")

    if tipo_usuario not in {"aluno", "professor"}:
        return redirect("register")

    form = RegisterForm(
        request.POST or None,
        tipo_usuario=tipo_usuario,
    )

    if request.method == "POST" and form.is_valid():
        resultado = AuthenticationService().register(
            nome=form.cleaned_data["nome"],
            email=form.cleaned_data["email"],
            senha=form.cleaned_data["senha"],
            tipo_usuario=tipo_usuario,
            codigo_professor=form.cleaned_data["codigo_professor"],
        )

        if resultado.success:
            messages.success(
                request,
                "Cadastro realizado com sucesso. Faça login para continuar.",
            )
            return redirect("login")

        form.add_error(
            None,
            resultado.message or "Não foi possível realizar o cadastro.",
        )

    return render(
        request,
        "authentication/register.html",
        {
            "form": form,
            "tipo_usuario": tipo_usuario,
        },
    )


def app_view(request, subpath=None):
    if not _sessao_valida(request):
        return redirect("login")

    token = request.session["auth_token"]

    shell_config = {
        "isAuthenticated": True,
        "accessToken": token,
        "logoutUrl": reverse("logout"),
    }

    return render(
        request,
        "authentication/app.html",
        {
            "shell_config": shell_config,
        },
    )


def dashboard_view(request):
    if not _sessao_valida(request):
        return redirect("login")

    return redirect("app")


def logout_view(request):
    request.session.flush()
    return redirect("login")
