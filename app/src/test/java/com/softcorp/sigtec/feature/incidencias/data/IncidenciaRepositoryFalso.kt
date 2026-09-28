package com.softcorp.sigtec.feature.incidencias.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository

/** Guarda en memoria lo que recibiría Room; con fallar = true simula un error al escribir. */
class IncidenciaRepositoryFalso(var fallar: Boolean = false) : IncidenciaRepository {

    val incidencias = mutableListOf<Incidencia>()
    val movimientos = mutableListOf<Movimiento>()

    override suspend fun registrar(incidencia: Incidencia, movimiento: Movimiento): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        incidencias += incidencia
        movimientos += movimiento
        return Resultado.Exito(Unit)
    }
}
