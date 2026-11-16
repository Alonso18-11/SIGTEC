package com.softcorp.sigtec.feature.indicadores.data

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.notificaciones.NotificadorAvisosArea
import com.softcorp.sigtec.core.sync.TareaDiaria
import com.softcorp.sigtec.core.sync.TipoTareaDiaria
import com.softcorp.sigtec.feature.indicadores.domain.CalcularIndicadoresDeHoy
import com.softcorp.sigtec.feature.indicadores.domain.IndicadoresRepository
import com.softcorp.sigtec.feature.indicadores.domain.contenidoCasosDemorados
import com.softcorp.sigtec.feature.indicadores.domain.contenidoResumenDia
import com.softcorp.sigtec.feature.indicadores.domain.reparadosHoy
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * HU-18 (RF-1812 a RF-1815): cada mañana, el aviso de casos demorados para el jefe, con la regla `esCritica` de la
 * HU-15. Con 0 no avisa y quita el del día anterior. No escribe `critica` (D-3).
 */
class TareaCasosDemorados @Inject constructor(
    private val calcular: CalcularIndicadoresDeHoy,
    private val repo: IndicadoresRepository,
    private val notificador: NotificadorAvisosArea
) : TareaDiaria {

    override val tipo = TipoTareaDiaria.CASOS_DEMORADOS
    override val necesitaDatosAlDia = true   // RF-1810

    override fun correPara(perfil: Perfil): Boolean = perfil.puede(Permiso.VER_INDICADORES)

    override suspend fun ultimaExitosa(): Instant? = repo.ultimaTareaExitosa(tipo.name)

    override suspend fun ejecutar(ahora: Instant, zona: ZoneId): Boolean {
        val indicadores = (calcular(ahora, zona) as? Resultado.Exito)?.valor ?: return false
        val aviso = contenidoCasosDemorados(indicadores.criticos)
        if (aviso == null) notificador.quitar(NotificadorAvisosArea.ID_CASOS_DEMORADOS)
        else notificador.avisar(NotificadorAvisosArea.ID_CASOS_DEMORADOS, aviso)
        return repo.registrarTarea(tipo.name, ahora) is Resultado.Exito
    }
}

/**
 * HU-18 (RF-1816 a RF-1818): al cierre de la jornada, el resumen del día para el jefe. No se guarda aparte: el tablero
 * ya lo calcula al abrirlo (D-5).
 */
class TareaResumenDia @Inject constructor(
    private val calcular: CalcularIndicadoresDeHoy,
    private val repo: IndicadoresRepository,
    private val notificador: NotificadorAvisosArea
) : TareaDiaria {

    override val tipo = TipoTareaDiaria.RESUMEN_DIA
    override val necesitaDatosAlDia = true   // RF-1810

    override fun correPara(perfil: Perfil): Boolean = perfil.puede(Permiso.VER_INDICADORES)

    override suspend fun ultimaExitosa(): Instant? = repo.ultimaTareaExitosa(tipo.name)

    override suspend fun ejecutar(ahora: Instant, zona: ZoneId): Boolean {
        val indicadores = (calcular(ahora, zona) as? Resultado.Exito)?.valor ?: return false
        notificador.avisar(
            NotificadorAvisosArea.ID_RESUMEN_DIA,
            contenidoResumenDia(indicadores.reparadosHoy, indicadores.pendientes, indicadores.criticos)
        )
        return repo.registrarTarea(tipo.name, ahora) is Resultado.Exito
    }
}
