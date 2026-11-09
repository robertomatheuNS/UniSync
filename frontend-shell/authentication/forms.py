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