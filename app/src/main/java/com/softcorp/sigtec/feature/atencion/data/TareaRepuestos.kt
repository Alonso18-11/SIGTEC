package com.softcorp.sigtec.feature.atencion.data

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Perfil
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.sync.TareaDiaria
import com.softcorp.sigtec.core.sync.TipoTareaDiaria
import com.softcorp.sigtec.feature.atencion.domain.ActualizarEstadoRepuestos
import com.softcorp.sigtec.feature.atencion.domain.AtencionRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * HU-18 (RF-1806 a RF-1809): la consulta diaria del estado de los repuestos en el celular del técnico. Es la misma
 * consulta de «Estado de repuestos» (HU-11), que ya guarda los cambios y anota `CONSULTAR_REPUESTOS`; por eso una
 * consulta hecha a mano ese día también cuenta (RF-1807). No avisa (D-7).
 */
class TareaRepuestos @Inject constructor(
    private val actualizar: ActualizarEstadoRepuestos,
    private val repo: AtencionRepository
) : TareaDiaria {

    override val tipo = TipoTareaDiaria.REPUESTOS
    override val necesitaDatosAlDia = false

    override fun correPara(perfil: Perfil): Boolean = perfil.puede(Permiso.ATENDER_INCIDENCIA)

    override suspend fun ultimaExitosa(): Instant? = repo.observarUltimaConsultaLogistica().first()

    // RF-1808: si alguna solicitud quedó sin respuesta, lo que sí respondió ya quedó guardado y se reintenta luego
    override suspend fun ejecutar(ahora: Instant, zona: ZoneId): Boolean =
        when (val r = actualizar(ahora)) {
            is Resultado.Exito -> r.valor.sinRespuesta == 0
            is Resultado.Error -> false
        }
}
