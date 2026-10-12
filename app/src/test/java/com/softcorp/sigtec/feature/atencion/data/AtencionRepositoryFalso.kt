package com.softcorp.sigtec.feature.atencion.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * Guarda en memoria lo que recibiría Room y lo expone como Flow, con el mismo orden que los DAO.
 * Con fallar = true simula un error al escribir (HU-08).
 */
class AtencionRepositoryFalso(var fallar: Boolean = false) : AtencionRepository {

    private val _incidencias = MutableStateFlow<List<Incidencia>>(emptyList())
    private val _movimientos = MutableStateFlow<List<Movimiento>>(emptyList())
    private val _informes = MutableStateFlow<List<InformeTecnico>>(emptyList())

    val incidencias: List<Incidencia> get() = _incidencias.value
    val movimientos: List<Movimiento> get() = _movimientos.value
    val informes: List<InformeTecnico> get() = _informes.value

    /** El `ahora` que recibió el último cerrarConInforme guardado. */
    var ultimoAhora: Instant? = null
        private set

    /** Carga datos de prueba sin pasar por cerrarConInforme(). */
    fun cargar(
        incidencias: List<Incidencia> = emptyList(),
        movimientos: List<Movimiento> = emptyList(),
        informes: List<InformeTecnico> = emptyList()
    ) {
        _incidencias.value = incidencias
        _movimientos.value = movimientos
        _informes.value = informes
    }

    override suspend fun incidencia(id: String): Incidencia? =
        _incidencias.value.firstOrNull { it.id == id }

    // RF-801: solo las del técnico que no estén cerradas, de la más antigua a la más reciente
    override fun observarBandeja(tecnicoId: String): Flow<List<Incidencia>> =
        _incidencias.map { lista ->
            lista.filter { it.tecnicoAsignadoId == tecnicoId && it.marca != MarcaSeguimiento.CERRADA }
                .sortedBy(Incidencia::fechaRegistro)
        }

    override suspend fun cerrarConInforme(
        incidencia: Incidencia,
        movimiento: Movimiento,
        informe: InformeTecnico,
        ahora: Instant
    ): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _incidencias.update { lista -> lista.map { if (it.id == incidencia.id) incidencia else it } }
        _movimientos.update { it + movimiento }
        _informes.update { it + informe }
        ultimoAhora = ahora
        return Resultado.Exito(Unit)
    }
}
