from django.shortcuts import redirect, render

from .forms import LoginForm
from .services.auth_service import AuthenticationService


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
        {
            "form": form,
        },
    )


def dashboard_view(request):
    if not request.session.get("auth_token"):
        return redirect("login")

    return render(request, "authentication/dashboard.html")


def logout_view(request):
    request.session.flush()
    return redirect("login")