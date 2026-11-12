from django.urls import path

from . import views


urlpatterns = [
    path("", views.home_view, name="home"),
    path("login/", views.login_view, name="login"),
    path("cadastro/", views.register_view, name="register"),
    path("dashboard/", views.dashboard_view, name="dashboard"),
    path("professor/", views.professor_view, name="professor"),
    path("logout/", views.logout_view, name="logout"),
]
