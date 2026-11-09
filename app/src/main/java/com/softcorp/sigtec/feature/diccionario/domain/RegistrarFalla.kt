package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Lo que escribe el técnico en «Registrar falla» (RF-1310). */
data class DatosFalla(
    val titulo: String,
    val tipo: TipoFalla?,
    val componente: String,
    val sintomas: String,
    val solucion: String
)

/**
 * HU-13 (RF-1313, RF-1314, RF-1318): guarda una falla nueva a nombre de quien tiene la sesión. Solo el técnico
 * (`REGISTRAR_FALLA`); los cinco campos son obligatorios (criterio de la HU).
 */
class RegistrarFalla @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: DiccionarioRepository
) {
    suspend operator fun invoke(
        datos: DatosFalla,
        ahora: Instant,
        incidenciaOrigenId: String? = null
    ): Resultado<FallaDiccionario> {
        val usuario = when (val r = verificarPermiso(Permiso.REGISTRAR_FALLA)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        val titulo = datos.titulo.trim()
        val componente = datos.componente.trim()
        val sintomas = datos.sintomas.trim()
        val solucion = datos.solucion.trim()
        val tipo = datos.tipo
        // La pantalla no deja guardar con campos vacíos (RF-1312); esto cubre cualquier otro llamador
        if (titulo.isEmpty() || componente.isEmpty() || sintomas.isEmpty() || solucion.isEmpty() || tipo == null) {
            return Resultado.Error(ErrorDominio.CampoObligatorio)
        }
        val falla = FallaDiccionario(
            id = UUID.randomUUID().toString(),
            titulo = titulo,
            sintomas = sintomas,
            componente = componente,
            tipo = tipo,
            solucion = solucion,
            autorId = usuario.id,
            autorNombre = usuario.nombre,
            registradaEn = ahora,
            incidenciaOrigenId = incidenciaOrigenId
        )
        return when (val r = repo.guardar(falla)) {
            is Resultado.Exito -> Resultado.Exito(falla)
            is Resultado.Error -> r
        }
    }
}
