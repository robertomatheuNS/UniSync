package br.com.unisync.mobile.domain.academico

data class Turma(val id: String, val nome: String, val disciplina: String, val periodo: String,
    val professorId: String, val professor: String, val codigo: String?, val totalAlunos: Int)

data class Aviso(val id: String, val turmaId: String, val turmaNome: String, val disciplina: String,
    val professorId: String, val professor: String, val titulo: String, val conteudo: String,
    val criadoEm: String, val atualizadoEm: String, val lido: Boolean)

data class DadosTurma(val nome: String, val disciplina: String, val periodo: String = "") {
    fun validar(): DadosTurma {
        require(nome.trim().length in 3..80) { "O nome da turma deve ter entre 3 e 80 caracteres." }
        require(disciplina.trim().length in 2..100) { "A disciplina deve ter entre 2 e 100 caracteres." }
        require(periodo.trim().length <= 30) { "O período deve ter até 30 caracteres." }
        return copy(nome = nome.trim(), disciplina = disciplina.trim(), periodo = periodo.trim())
    }
}

data class DadosAviso(val turmaId: String, val titulo: String, val conteudo: String) {
    fun validar(): DadosAviso {
        require(turmaId.isNotBlank()) { "Selecione uma turma." }
        require(titulo.trim().length in 3..100) { "O título deve ter entre 3 e 100 caracteres." }
        require(conteudo.trim().length in 1..5000) { "O aviso deve ter entre 1 e 5000 caracteres." }
        return copy(titulo = titulo.trim(), conteudo = conteudo.trim())
    }
}

fun validarCodigoTurma(codigo: String): String = codigo.trim().uppercase().also {
    require(it.matches(Regex("[A-F0-9]{12}"))) { "Informe o código de 12 caracteres fornecido pelo professor." }
}

data class ConteudoAcademico(val turmas: List<Turma> = emptyList(), val avisos: List<Aviso> = emptyList())

interface Academico {
    suspend fun carregar(token: String): ConteudoAcademico
    suspend fun criarTurma(token: String, dados: DadosTurma): Turma
    suspend fun editarTurma(token: String, id: String, dados: DadosTurma): Turma
    suspend fun entrarTurma(token: String, codigo: String): Turma
    suspend fun arquivarTurma(token: String, id: String)
    suspend fun publicarAviso(token: String, dados: DadosAviso): Aviso
    suspend fun editarAviso(token: String, id: String, dados: DadosAviso): Aviso
    suspend fun excluirAviso(token: String, id: String)
    suspend fun marcarLido(token: String, id: String): Aviso
}
