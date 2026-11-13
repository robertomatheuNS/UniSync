package br.com.unisync.mobile.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import br.com.unisync.mobile.domain.auth.ArmazenamentoSessao
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Somente texto cifrado no disco; a chave permanece no Android Keystore. */
class SessaoSegura(contexto: Context, nomeArquivo: String = "sessao_segura") : ArmazenamentoSessao {
    private val preferencias = contexto.getSharedPreferences(nomeArquivo, Context.MODE_PRIVATE)

    private fun chave(): SecretKey {
        val cofre = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (cofre.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    override suspend fun lerToken(): String? = withContext(Dispatchers.IO) {
        val registro = preferencias.getString("token", null) ?: return@withContext null
        val partes = registro.split(":")
        require(partes.size == 2)
        val cifra = Cipher.getInstance("AES/GCM/NoPadding")
        cifra.init(Cipher.DECRYPT_MODE, chave(), GCMParameterSpec(128, Base64.getDecoder().decode(partes[0])))
        String(cifra.doFinal(Base64.getDecoder().decode(partes[1])), Charsets.UTF_8)
    }

    override suspend fun salvarToken(token: String) = withContext(Dispatchers.IO) {
        val cifra = Cipher.getInstance("AES/GCM/NoPadding")
        cifra.init(Cipher.ENCRYPT_MODE, chave())
        val encoder = Base64.getEncoder()
        val registro = encoder.encodeToString(cifra.iv) + ":" + encoder.encodeToString(cifra.doFinal(token.toByteArray(Charsets.UTF_8)))
        if (!preferencias.edit().putString("token", registro).commit()) throw IOException("Falha ao salvar sessão")
    }

    override suspend fun limpar() = withContext(Dispatchers.IO) {
        if (!preferencias.edit().clear().commit()) throw IOException("Falha ao remover sessão")
    }

    private companion object { const val ALIAS = "unisync.sessao.v1" }
}
