package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import kotlinx.coroutines.flow.Flow

/** HU-09 (RF-925): la foto que se va a tomar, antes de que la cámara la escriba en [ruta]. */
data class FotoNueva(val id: String, val ruta: String)

/**
 * HU-09: las fotos de evidencia de una incidencia. Viven solo en este celular, en Room y en la carpeta interna,
 * y nunca se sincronizan (RF-939, RF-940).
 */
interface EvidenciaRepository {
    /** La incidencia tal como está en Room, para notar si se cierra o se reasigna (RF-931). */
    fun observarIncidencia(incidenciaId: String): Flow<Incidencia?>

    /** Sus fotos, de la más antigua a la más reciente. */
    fun observarEvidencias(incidenciaId: String): Flow<List<Evidencia>>

    /** Lectura fresca de la incidencia, justo antes de enlazar una foto (RF-926). */
    suspend fun incidencia(incidenciaId: String): Incidencia?

    /** Cuántas fotos tiene ya la incidencia en este celular (RF-929). */
    suspend fun contarEvidencias(incidenciaId: String): Int

    /** Dónde escribirá la cámara la foto [id]; prepara la carpeta de la incidencia (RF-925). */
    suspend fun rutaParaNuevaFoto(incidenciaId: String, id: String): String

    /** Enlaza la foto a la incidencia; si no puede, borra su archivo (RF-928). */
    suspend fun guardar(evidencia: Evidencia): Resultado<Unit>

    /** Borra el archivo de una foto que no se enlazó; si ya no existe, no pasa nada (RF-928). */
    suspend fun descartarArchivo(ruta: String)
}
