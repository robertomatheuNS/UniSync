from django.contrib import messages
from django.shortcuts import redirect, render

from .forms import LoginForm, RegisterForm
from .services.auth_service import AuthenticationService


def home_view(request):
    return render(
        request,
        "authentication/home.html",
    )


def login_view(request):
    if request.session.get("auth_token"):
        return redirect("dashboard")

    form = LoginForm(request.POST or None)

    if request.method == "POST" and form.is_valid():
        service = AuthenticationService()

        resultado = service.login(
            email=form.cleaned_data["email"],
            senha=form.cleaned_data["senha"],
        )

        if resultado.success and resultado.token:
            request.session["auth_token"] = resultado.token
            return redirect("dashboard")

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
    if request.session.get("auth_token"):
        return redirect("dashboard")

    return render(
        request,
        "authentication/register_choice.html",
    )


def register_view(request, tipo_usuario):
    if request.session.get("auth_token"):
        return redirect("dashboard")

    if tipo_usuario not in {"aluno", "professor"}:
        return redirect("register")

    form = RegisterForm(
        request.POST or None,
        tipo_usuario=tipo_usuario,
    )

    if request.method == "POST" and form.is_valid():
        service = AuthenticationService()

        resultado = service.register(
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


def app_view(request):
    if not request.session.get("auth_token"):
        return redirect("login")

    return render(
        request,
        "authentication/app.html",
    )


def dashboard_view(request):
    if not request.session.get("auth_token"):
        return redirect("login")

    return render(
        request,
        "authentication/dashboard.html",
    )


def logout_view(request):
    request.session.flush()
    return redirect("login")