package com.softcorp.sigtec.feature.ia.data

import android.util.Log
import com.google.firebase.ai.GenerativeModel
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.sync.MonitorConexion
import com.softcorp.sigtec.feature.ia.di.MODELO_GEMINI
import com.softcorp.sigtec.feature.ia.di.ModeloBorrador
import com.softcorp.sigtec.feature.ia.di.ModeloClasificacion
import com.softcorp.sigtec.feature.ia.domain.AsistenteIA
import com.softcorp.sigtec.feature.ia.domain.EntradaBorrador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * HU-14 (plan 4.6): el asistente con Gemini por Firebase AI Logic. Sin conexión no llama (`SinConexion`); si Gemini no
 * responde a tiempo, falla, se agotó la cuota o App Check lo rechaza, devuelve `IANoDisponible` (RF-1406, RF-1423).
 */
class AsistenteIAGemini @Inject constructor(
    @param:ModeloClasificacion private val modeloClasificacion: GenerativeModel,
    @param:ModeloBorrador private val modeloBorrador: GenerativeModel,
    private val monitorConexion: MonitorConexion,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : AsistenteIA {

    override val modelo: String = MODELO_GEMINI

    override val disponible: Flow<Boolean> = monitorConexion.hayConexion

    override suspend fun clasificar(descripcion: String): Resultado<String> =
        pedir(modeloClasificacion, pedidoClasificacion(descripcion), "clasificar")

    override suspend fun redactarBorrador(entrada: EntradaBorrador): Resultado<String> = pedir(
        modeloBorrador,
        pedidoBorrador(
            entrada.descripcion, entrada.notas, entrada.diagnostico, entrada.trabajoRealizado,
            entrada.piezasCambiadas, entrada.observaciones
        ),
        "redactar el borrador"
    )

    private suspend fun pedir(modelo: GenerativeModel, pedido: String, que: String): Resultado<String> {
        if (!monitorConexion.hayConexionAhora()) return Resultado.Error(ErrorDominio.SinConexion)
        return withContext(io) {
            try {
                val texto = modelo.generateContent(pedido).text
                if (texto.isNullOrBlank()) Resultado.Error(ErrorDominio.IANoDisponible) else Resultado.Exito(texto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gemini no pudo $que", e)
                Resultado.Error(ErrorDominio.IANoDisponible)
            }
        }
    }

    private companion object {
        const val TAG = "AsistenteIA"
    }
}
