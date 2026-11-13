package br.com.unisync.mobile.domain.auth

import br.com.unisync.mobile.data.SessaoAutenticada

interface Autenticacao {
    suspend fun entrar(email: String, senha: String): SessaoAutenticada
    suspend fun cadastrar(nome: String, email: String, senha: String, codigoProfessor: String?)
}

interface ArmazenamentoSessao {
    suspend fun lerToken(): String?
    suspend fun salvarToken(token: String)
    suspend fun limpar()
}
