package com.softcorp.sigtec.feature.incidencias.data

import android.util.Log
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.feature.incidencias.domain.DirectorioRepository
import com.softcorp.sigtec.feature.incidencias.domain.ErrorDirectorio
import com.softcorp.sigtec.feature.incidencias.domain.ResultadoDirectorio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** HU-12: el directorio con Retrofit, siempre en una corrutina fuera del hilo principal (sección 14). */
class DirectorioRepositoryImpl @Inject constructor(
    private val api: DirectorioApi,
    @IoDispatcher private val io: CoroutineDispatcher
) : DirectorioRepository {

    override suspend fun responsableDe(codigoEquipo: String): ResultadoDirectorio = withContext(io) {
        try {
            val coincidencias = api.buscarPorEquipo(codigoEquipo.trim()).coincidenciasExactas(codigoEquipo)
            if (coincidencias.size > 1) {
                Log.w(TAG, "El directorio tiene ${coincidencias.size} responsables para $codigoEquipo; se usa el primero")
            }
            coincidencias.firstOrNull()?.let { ResultadoDirectorio.Encontrado(it) }
                ?: ResultadoDirectorio.Fallo(ErrorDirectorio.NO_ENCONTRADO)   // RF-1211
        } catch (e: CancellationException) {
            throw e   // RF-1204: el código cambió y la consulta ya no importa
        } catch (e: Exception) {
            val error = errorDeDirectorio(e)
            if (error == ErrorDirectorio.SIN_RESPUESTA) Log.w(TAG, "No se pudo consultar el directorio", e)   // RF-1212
            ResultadoDirectorio.Fallo(error)
        }
    }

    private companion object {
        const val TAG = "DirectorioRepository"
    }
}
