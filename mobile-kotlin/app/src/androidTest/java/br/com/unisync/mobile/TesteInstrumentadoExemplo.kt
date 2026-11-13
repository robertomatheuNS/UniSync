package br.com.unisync.mobile

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TesteInstrumentadoExemplo {
    @Test
    fun verificaContextoAplicativo() {
        // Context of the app under test.
        val contextoAplicativo = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("br.com.unisync.mobile", contextoAplicativo.packageName)
    }
}