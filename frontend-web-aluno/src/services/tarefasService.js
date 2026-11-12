
const API_URL =
    import.meta.env.VITE_PYTHON_API_URL || "http://localhost:8000";

async function requisicaoTarefas(caminho, token, opcoes = {}) {
    const resposta = await fetch(`${API_URL}${caminho}`, {
        ...opcoes,
        headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
        ...opcoes.headers,
        },
    });

    if (!resposta.ok) {
        const erro = await resposta.json().catch(() => ({}));

    throw new Error(
        typeof erro.detail === "string"
            ? erro.detail
            : `Erro HTTP ${resposta.status}`
        );
    }

    if (resposta.status === 204) {
        return null;
    }

    return resposta.json();
}

export function listarTarefas(token) {
    return requisicaoTarefas("/tarefas/listar", token);
}

export function criarTarefa(titulo, token) {
    return requisicaoTarefas("/tarefas/criar", token, {
        method: "POST",
        body: JSON.stringify({ titulo }),
    });
    }

export function atualizarTarefa(id, dados, token) {
    return requisicaoTarefas(
        `/tarefas/atualizar/${id}`,
        token,
        {
        method: "PATCH",
        body: JSON.stringify(dados),
        }
    );
}

export function excluirTarefa(id, token) {
    return requisicaoTarefas(
        `/tarefas/excluir/${id}`,
        token,
        { method: "DELETE" }
    );
}
