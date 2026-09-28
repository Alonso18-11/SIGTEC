package com.softcorp.sigtec.feature.incidencias.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Guarda en memoria lo que recibiría Room y lo expone como Flow, con el mismo orden que los DAO.
 * Con fallar = true simula un error al escribir.
 */
class IncidenciaRepositoryFalso(var fallar: Boolean = false) : IncidenciaRepository {

    private val _incidencias = MutableStateFlow<List<Incidencia>>(emptyList())
    private val _movimientos = MutableStateFlow<List<Movimiento>>(emptyList())

    val incidencias: List<Incidencia> get() = _incidencias.value
    val movimientos: List<Movimiento> get() = _movimientos.value

    /** Carga datos de prueba sin pasar por registrar(). */
    fun cargar(incidencias: List<Incidencia> = emptyList(), movimientos: List<Movimiento> = emptyList()) {
        _incidencias.value = incidencias
        _movimientos.value = movimientos
    }

    override suspend fun registrar(incidencia: Incidencia, movimiento: Movimiento): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _incidencias.update { it + incidencia }
        _movimientos.update { it + movimiento }
        return Resultado.Exito(Unit)
    }

    override fun observarTodas(): Flow<List<Incidencia>> =
        _incidencias.map { lista -> lista.sortedByDescending(Incidencia::fechaRegistro) }

    override fun observarPorId(id: String): Flow<Incidencia?> =
        _incidencias.map { lista -> lista.firstOrNull { it.id == id } }

    override fun observarMovimientos(incidenciaId: String): Flow<List<Movimiento>> =
        _movimientos.map { lista ->
            lista.filter { it.incidenciaId == incidenciaId }.sortedByDescending(Movimiento::fecha)
        }
}
