from django import forms


class LoginForm(forms.Form):
    email = forms.EmailField(
        label="E-mail",
        required=True,
        widget=forms.EmailInput(
            attrs={
                "id": "email",
                "name": "email",
                "autocomplete": "email",
            }
        ),
    )

    senha = forms.CharField(
        label="Senha",
        required=True,
        widget=forms.PasswordInput(
            attrs={
                "id": "senha",
                "name": "senha",
                "autocomplete": "current-password",
            }
        ),
    )

    def clean_email(self):
        return self.cleaned_data["email"].strip().lower()


class RegisterForm(forms.Form):
    nome = forms.CharField(
        label="Nome completo",
        min_length=2,
        max_length=100,
        required=True,
        widget=forms.TextInput(
            attrs={
                "id": "nome",
                "name": "nome",
                "autocomplete": "name",
            }
        ),
    )

    email = forms.EmailField(
        label="E-mail",
        max_length=180,
        required=True,
        widget=forms.EmailInput(
            attrs={
                "id": "email",
                "name": "email",
                "autocomplete": "email",
            }
        ),
    )

    senha = forms.CharField(
        label="Senha",
        min_length=8,
        max_length=128,
        required=True,
        widget=forms.PasswordInput(
            attrs={
                "id": "senha",
                "name": "senha",
                "autocomplete": "new-password",
            }
        ),
    )

    def clean_nome(self):
        return " ".join(self.cleaned_data["nome"].split())

    def clean_email(self):
        return self.cleaned_data["email"].strip().lower()
