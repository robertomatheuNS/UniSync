package br.com.unisync.mobile.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.unisync.mobile.data.SessaoAutenticada

class ModeloSessao : ViewModel() {
    var sessao by mutableStateOf<SessaoAutenticada?>(null)
        private set

    fun iniciar(sessao: SessaoAutenticada) {
        this.sessao = sessao
    }

    fun encerrar() {
        sessao = null
    }
}
