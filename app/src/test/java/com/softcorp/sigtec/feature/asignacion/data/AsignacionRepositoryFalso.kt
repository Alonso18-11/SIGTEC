package com.softcorp.sigtec.feature.asignacion.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.Tecnico
import com.softcorp.sigtec.feature.asignacion.domain.AsignacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * HU-06: guarda en memoria lo que recibiría Room y lo expone como Flow, con el mismo orden que los
 * DAO. Con fallar = true, `asignar` simula un error de Room y no guarda nada.
 */
class AsignacionRepositoryFalso(var fallar: Boolean = false) : AsignacionRepository {

    private val _incidencias = MutableStateFlow<List<Incidencia>>(emptyList())
    private val _movimientos = MutableStateFlow<List<Movimiento>>(emptyList())
    private val _tecnicos = MutableStateFlow<List<Tecnico>>(emptyList())
    private val _sinSubir = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val oficinas = mutableMapOf<String, String>()

    val incidencias: List<Incidencia> get() = _incidencias.value
    val movimientos: List<Movimiento> get() = _movimientos.value

    /** El `ahora` que recibió la última asignación guardada. */
    var ultimoAhora: Instant? = null
        private set

    /** Carga datos de prueba sin pasar por asignar(). */
    fun cargar(
        incidencias: List<Incidencia> = emptyList(),
        tecnicos: List<Tecnico> = emptyList(),
        oficinas: Map<String, String> = emptyMap()
    ) {
        _incidencias.value = incidencias
        _tecnicos.value = tecnicos
        this.oficinas.putAll(oficinas)
    }

    /** RF-602: lo que contaría la consulta de Room de asignaciones sin subir. */
    fun fijarSinSubir(porTecnico: Map<String, Int>) {
        _sinSubir.value = porTecnico
    }

    override suspend fun incidencia(id: String): Incidencia? = _incidencias.value.firstOrNull { it.id == id }

    override fun observarIncidencia(id: String): Flow<Incidencia?> =
        _incidencias.map { lista -> lista.firstOrNull { it.id == id } }

    override suspend fun tecnico(id: String): Tecnico? = _tecnicos.value.firstOrNull { it.id == id }

    override fun observarTecnicos(): Flow<List<Tecnico>> = _tecnicos.map { it.sortedBy(Tecnico::nombre) }

    override suspend fun oficinaDeEquipo(codigo: String): String? = oficinas[codigo]

    override fun observarAsignacionesSinSubir(): Flow<Map<String, Int>> = _sinSubir

    override suspend fun asignacionesSinSubir(tecnicoId: String): Int = _sinSubir.value[tecnicoId] ?: 0

    override suspend fun asignar(incidencia: Incidencia, movimiento: Movimiento, ahora: Instant): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _incidencias.update { lista -> lista.map { if (it.id == incidencia.id) incidencia else it } }
        _movimientos.update { it + movimiento }
        ultimoAhora = ahora
        return Resultado.Exito(Unit)
    }
}
