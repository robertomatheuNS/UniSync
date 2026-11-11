document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("form");
    const button = form?.querySelector("button[type='submit']");

    if (!form || !button) {
        return;
    }

    form.addEventListener("submit", () => {
        button.disabled = true;
        button.textContent = "Processando...";
    });
});