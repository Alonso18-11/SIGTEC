package com.softcorp.sigtec.feature.captura.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import javax.inject.Inject

/**
 * HU-09: enlaza a la incidencia la foto que la cámara ya escribió en `foto.ruta` (RF-925). Vuelve a verificar
 * todo con la foto ya tomada, porque la incidencia pudo cerrarse o reasignarse mientras tanto (RF-926, P-10).
 * Si no la enlaza, borra el archivo (RF-928). [ahora] lo pasa el ViewModel: este caso de uso no llama a now().
 */
class GuardarEvidencia @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: EvidenciaRepository
) {
    suspend operator fun invoke(
        incidenciaId: String,
        foto: FotoNueva,
        tipo: TipoEvidencia,
        ahora: Instant
    ): Resultado<Evidencia> {
        val usuario = when (val r = verificarPermiso(Permiso.ATENDER_INCIDENCIA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> {
                repo.descartarArchivo(foto.ruta)
                return r
            }
        }
        // Lectura fresca: el estado de la pantalla puede ser de antes de disparar
        val incidencia = repo.incidencia(incidenciaId)
        val cantidad = repo.contarEvidencias(incidenciaId)
        bloqueoParaFotos(usuario, incidencia, cantidad)?.let { bloqueo ->
            repo.descartarArchivo(foto.ruta)
            return Resultado.Error(bloqueo)
        }

        val evidencia = Evidencia(foto.id, incidenciaId, tipo, foto.ruta, fechaCaptura = ahora)
        // Si Room falla, el repositorio ya borró el archivo (RF-928)
        return when (val r = repo.guardar(evidencia)) {
            is Resultado.Exito -> Resultado.Exito(evidencia)
            is Resultado.Error -> r
        }
    }
}
