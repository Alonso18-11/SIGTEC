package com.softcorp.sigtec.feature.incidencias.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.feature.incidencias.domain.IncidenciaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Guarda en memoria lo que recibiría Room y lo expone como Flow, con el mismo orden que los DAO.
 * Con fallar = true simula un error al escribir.
 */
class IncidenciaRepositoryFalso(var fallar: Boolean = false) : IncidenciaRepository {

    private val _incidencias = MutableStateFlow<List<Incidencia>>(emptyList())
    private val _movimientos = MutableStateFlow<List<Movimiento>>(emptyList())
    private val _informes = MutableStateFlow<List<InformeTecnico>>(emptyList())
    private val _evidencias = MutableStateFlow<List<Evidencia>>(emptyList())   // HU-09

    // RF-41: catálogo en memoria, con los 4 equipos de prueba reales por defecto.
    private val equiposValidos: MutableSet<String> = mutableSetOf(
        "PC-CONT-014", "PC-LOG-009", "PC-SIS-003", "IMP-RRHH-002"
    )

    val incidencias: List<Incidencia> get() = _incidencias.value
    val movimientos: List<Movimiento> get() = _movimientos.value
    val informes: List<InformeTecnico> get() = _informes.value

    /** El `ahora` que recibió el último iniciarAtencion guardado (HU-08). */
    var ultimoAhora: Instant? = null
        private set

    /** Carga datos de prueba sin pasar por registrar(). */
    fun cargar(
        incidencias: List<Incidencia> = emptyList(),
        movimientos: List<Movimiento> = emptyList(),
        informes: List<InformeTecnico> = emptyList(),
        evidencias: List<Evidencia> = emptyList()
    ) {
        _incidencias.value = incidencias
        _movimientos.value = movimientos
        _informes.value = informes
        _evidencias.value = evidencias
    }

    /** Amplía el catálogo falso para pruebas que necesiten un código distinto a los 4 por defecto. */
    fun agregarEquipo(codigo: String) {
        equiposValidos.add(codigo)
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

    override suspend fun existeEquipo(codigo: String): Boolean = codigo in equiposValidos

    // HU-08: lectura fresca para revalidar la marca justo antes de guardar (RF-807, RF-808)
    override suspend fun porId(id: String): Incidencia? = _incidencias.value.firstOrNull { it.id == id }

    override suspend fun iniciarAtencion(
        incidencia: Incidencia,
        movimiento: Movimiento,
        ahora: Instant
    ): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _incidencias.update { lista -> lista.map { if (it.id == incidencia.id) incidencia else it } }
        _movimientos.update { it + movimiento }
        ultimoAhora = ahora
        return Resultado.Exito(Unit)
    }

    override fun observarInforme(incidenciaId: String): Flow<InformeTecnico?> =
        _informes.map { lista -> lista.firstOrNull { it.incidenciaId == incidenciaId } }

    // Mismo orden que EvidenciaDao: de la más antigua a la más reciente
    override fun observarEvidencias(incidenciaId: String): Flow<List<Evidencia>> =
        _evidencias.map { lista -> lista.filter { it.incidenciaId == incidenciaId }.sortedBy(Evidencia::fechaCaptura) }
}
