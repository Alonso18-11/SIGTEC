package com.softcorp.sigtec.feature.incidencias.data

import com.softcorp.sigtec.feature.incidencias.domain.Colaborador
import com.softcorp.sigtec.feature.incidencias.domain.DirectorioRepository
import com.softcorp.sigtec.feature.incidencias.domain.ErrorDirectorio
import com.softcorp.sigtec.feature.incidencias.domain.ResultadoDirectorio
import kotlinx.coroutines.CompletableDeferred

/**
 * HU-12: el directorio en memoria, con los colaboradores del servidor de prueba. Con [falla] simula «sin respuesta»;
 * con [enEspera] la consulta queda colgada hasta completarlo, para probar «Buscando…» y la cancelación.
 */
class DirectorioRepositoryFalso(
    val colaboradores: MutableMap<String, Colaborador> = mutableMapOf(
        "PC-CONT-014" to Colaborador("María Quispe Huamán", "Contabilidad", "2314"),
        "PC-LOG-009" to Colaborador("Juan Paredes Ríos", "Logística", "2120")
    )
) : DirectorioRepository {

    var falla: ErrorDirectorio? = null
    var enEspera: CompletableDeferred<Unit>? = null

    /** Cada código consultado, en orden. */
    val consultas = mutableListOf<String>()

    override suspend fun responsableDe(codigoEquipo: String): ResultadoDirectorio {
        consultas += codigoEquipo
        enEspera?.await()
        falla?.let { return ResultadoDirectorio.Fallo(it) }
        return colaboradores[codigoEquipo.trim().uppercase()]?.let { ResultadoDirectorio.Encontrado(it) }
            ?: ResultadoDirectorio.Fallo(ErrorDirectorio.NO_ENCONTRADO)
    }
}
