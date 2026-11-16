package com.softcorp.sigtec.feature.ia.domain

import com.softcorp.sigtec.core.domain.Resultado
import kotlinx.coroutines.flow.Flow

/**
 * HU-14: el asistente de IA (Gemini por Firebase AI Logic). Devuelve el texto tal como llega; el dominio lo valida con
 * [leerClasificacion] y [leerBorrador]. Si no hay conexión devuelve `SinConexion`, y si Gemini no responde a tiempo,
 * falla o se agotó la cuota, `IANoDisponible` (RF-1406, RF-1423).
 */
interface AsistenteIA {
    /** El modelo que genera las sugerencias; se guarda en cada una (RF-1427). */
    val modelo: String

    /** Si hay conexión para pedirle algo (RF-1415). */
    val disponible: Flow<Boolean>

    suspend fun clasificar(descripcion: String): Resultado<String>

    suspend fun redactarBorrador(entrada: EntradaBorrador): Resultado<String>
}
