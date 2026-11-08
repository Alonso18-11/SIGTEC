package com.softcorp.sigtec.feature.atencion.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.InformeTecnico
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Movimiento
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import com.softcorp.sigtec.feature.atencion.domain.SolicitudConIncidencia
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
    private val _solicitudes = MutableStateFlow<List<SolicitudRepuesto>>(emptyList())

    val incidencias: List<Incidencia> get() = _incidencias.value
    val movimientos: List<Movimiento> get() = _movimientos.value
    val informes: List<InformeTecnico> get() = _informes.value
    val solicitudes: List<SolicitudRepuesto> get() = _solicitudes.value

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

    // HU-11: como Room, o se guarda todo o nada
    override suspend fun solicitarRepuesto(
        incidencia: Incidencia,
        movimiento: Movimiento,
        solicitud: SolicitudRepuesto,
        ahora: Instant
    ): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _incidencias.update { lista -> lista.map { if (it.id == incidencia.id) incidencia else it } }
        _movimientos.update { it + movimiento }
        _solicitudes.update { it + solicitud }
        ultimoAhora = ahora
        return Resultado.Exito(Unit)
    }

    override suspend fun solicitudesPorRegistrar(): List<SolicitudRepuesto> =
        _solicitudes.value.filter { it.numeroLogistica == null && !it.rechazadaPorLogistica }

    override suspend fun marcarRegistrada(solicitudId: String, numero: String, ahora: Instant): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _solicitudes.update { lista -> lista.map { if (it.id == solicitudId) it.copy(numeroLogistica = numero) else it } }
        return Resultado.Exito(Unit)
    }

    override suspend fun marcarRechazada(solicitudId: String, ahora: Instant): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _solicitudes.update { lista -> lista.map { if (it.id == solicitudId) it.copy(rechazadaPorLogistica = true) else it } }
        return Resultado.Exito(Unit)
    }

    /** Carga solicitudes de prueba sin pasar por solicitarRepuesto(). */
    fun cargarSolicitudes(solicitudes: List<SolicitudRepuesto>) {
        _solicitudes.value = solicitudes
    }

    private val _ultimaConsulta = MutableStateFlow<Instant?>(null)

    /** Cada consulta a Logística registrada: el instante y si fue correcta. */
    val consultas = mutableListOf<Pair<Instant, Boolean>>()

    override suspend fun solicitudesSinEntregarDe(tecnicoId: String): List<SolicitudRepuesto> =
        _solicitudes.value.filter {
            it.solicitadoPorId == tecnicoId && it.numeroLogistica != null && !it.rechazadaPorLogistica &&
                it.estadoEntrega != com.softcorp.sigtec.core.domain.model.EstadoEntrega.ENTREGADO
        }

    override suspend fun contarSinEntregarDe(incidenciaId: String): Int =
        _solicitudes.value.count {
            it.incidenciaId == incidenciaId && !it.rechazadaPorLogistica &&
                it.estadoEntrega != com.softcorp.sigtec.core.domain.model.EstadoEntrega.ENTREGADO
        }

    override suspend fun aplicarEstado(
        solicitud: SolicitudRepuesto,
        incidencia: Incidencia?,
        movimiento: Movimiento?,
        ahora: Instant
    ): Resultado<Unit> {
        if (fallar) return Resultado.Error(ErrorDominio.Desconocido("fallo simulado"))
        _solicitudes.update { lista -> lista.map { if (it.id == solicitud.id) solicitud else it } }
        incidencia?.let { nueva -> _incidencias.update { lista -> lista.map { if (it.id == nueva.id) nueva else it } } }
        movimiento?.let { m -> _movimientos.update { it + m } }
        return Resultado.Exito(Unit)
    }

    override suspend fun registrarConsultaLogistica(ahora: Instant, exitosa: Boolean) {
        consultas += ahora to exitosa
        if (exitosa) _ultimaConsulta.value = ahora
    }

    override fun observarUltimaConsultaLogistica(): Flow<Instant?> = _ultimaConsulta

    override fun observarSolicitudesDe(tecnicoId: String): Flow<List<SolicitudConIncidencia>> =
        kotlinx.coroutines.flow.combine(_solicitudes, _incidencias) { solicitudes, incidencias ->
            solicitudes.filter { it.solicitadoPorId == tecnicoId }
                .sortedByDescending { it.fechaSolicitud }
                .map { s ->
                    val incidencia = incidencias.firstOrNull { it.id == s.incidenciaId }
                    SolicitudConIncidencia(s, incidencia?.codigoVisible, incidencia?.codigoEquipo.orEmpty())
                }
        }
}
