package com.softcorp.sigtec.feature.atencion.data

import android.util.Log
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.domain.EstadoEnLogistica
import com.softcorp.sigtec.feature.atencion.domain.LogisticaRepository
import com.softcorp.sigtec.feature.atencion.domain.PiezaLogistica
import com.softcorp.sigtec.feature.atencion.domain.ResultadoLogistica
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** HU-11 (plan 4.4): Logística con Retrofit, siempre en una corrutina fuera del hilo principal (sección 14). */
class LogisticaRepositoryImpl @Inject constructor(
    private val api: LogisticaApi,
    @IoDispatcher private val io: CoroutineDispatcher
) : LogisticaRepository {

    override suspend fun buscarPiezas(texto: String): ResultadoLogistica<List<PiezaLogistica>> =
        llamar("buscar piezas") {
            try {
                api.buscarPiezas(texto.trim()).mapNotNull { it.aDominio() }
            } catch (e: Exception) {
                if (esBusquedaSinResultados(e)) emptyList() else throw e   // RF-1107
            }
        }

    override suspend fun registrar(
        solicitud: SolicitudRepuesto,
        codigoIncidencia: String,
        codigoEquipo: String
    ): ResultadoLogistica<String> = llamar("registrar la solicitud ${solicitud.id}") {
        val registrada = api.registrar(solicitud.aLogistica(codigoIncidencia, codigoEquipo))
        registrada.id?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Logística no devolvió el número")
    }

    override suspend fun consultar(numero: String): ResultadoLogistica<EstadoEnLogistica> =
        llamar("consultar la solicitud $numero") {
            api.consultar(numero).aEstado() ?: throw IllegalArgumentException("Estado desconocido de Logística")
        }

    private suspend fun <T> llamar(que: String, bloque: suspend () -> T): ResultadoLogistica<T> = withContext(io) {
        try {
            ResultadoLogistica.Exito(bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = errorDeLogistica(e)
            Log.w(TAG, "No se pudo $que en Logística (${error.name})", e)
            ResultadoLogistica.Fallo(error)
        }
    }

    private companion object {
        const val TAG = "LogisticaRepository"
    }
}
