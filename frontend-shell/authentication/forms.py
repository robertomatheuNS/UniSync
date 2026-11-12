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
        required=True,
        min_length=2,
        max_length=100,
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
        required=True,
        max_length=180,
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
        min_length=8,
        max_length=128,
        widget=forms.PasswordInput(
            attrs={
                "id": "senha",
                "name": "senha",
                "autocomplete": "new-password",
            }
        ),
    )

    codigo_professor = forms.CharField(
        label="Código do professor",
        required=False,
        widget=forms.TextInput(
            attrs={
                "id": "codigo_professor",
                "name": "codigo_professor",
            }
        ),
    )

    def __init__(self, *args, tipo_usuario="aluno", **kwargs):
        super().__init__(*args, **kwargs)
        self.tipo_usuario = tipo_usuario

    def clean_nome(self):
        return " ".join(self.cleaned_data["nome"].split())

    def clean_email(self):
        return self.cleaned_data["email"].strip().lower()

    def clean(self):
        cleaned_data = super().clean()

        codigo_professor = cleaned_data.get(
            "codigo_professor",
            "",
        ).strip()

        if self.tipo_usuario == "professor" and not codigo_professor:
            self.add_error(
                "codigo_professor",
                "O código do professor é obrigatório.",
            )

        cleaned_data["codigo_professor"] = codigo_professor

        return cleaned_data