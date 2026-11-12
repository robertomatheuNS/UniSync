from django.urls import path

from . import views


urlpatterns = [
    path("", views.home_view, name="home"),
    path("login/", views.login_view, name="login"),
    path("cadastro/", views.register_choice_view, name="register"),
    path(
        "cadastro/<str:tipo_usuario>/",
        views.register_view,
        name="register_form",
    ),
    path("app/", views.app_view, name="app"),
    path(
    "app/<path:subpath>",
    views.app_view,
    name="app_nested",
),
    path("dashboard/", views.dashboard_view, name="dashboard"),
    path("logout/", views.logout_view, name="logout"),
]