package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.domain.model.Permiso
import com.softcorp.sigtec.core.domain.usecase.VerificarPermiso
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Lo que ve quien abre el diccionario: las fallas y si puede registrar (RF-1301, RF-1309). */
data class AccesoDiccionario(
    val fallas: Flow<List<FallaDiccionario>>,
    val puedeRegistrar: Boolean
)

/**
 * HU-13 (RF-1301, RF-1308): solo con `CONSULTAR_DICCIONARIO` (técnico y personal de sistemas); el jefe recibe
 * `SinPermiso` aunque llegue a la pantalla. Lo usan el diccionario y las sugerencias del detalle (RF-1330).
 */
class ObservarDiccionario @Inject constructor(
    private val verificarPermiso: VerificarPermiso,
    private val repo: DiccionarioRepository
) {
    operator fun invoke(): Resultado<AccesoDiccionario> {
        val usuario = when (val r = verificarPermiso(Permiso.CONSULTAR_DICCIONARIO)) {
            is Resultado.Exito -> r.valor
            is Resultado.Error -> return r
        }
        return Resultado.Exito(
            AccesoDiccionario(repo.observarTodas(), puedeRegistrar = usuario.perfil.puede(Permiso.REGISTRAR_FALLA))
        )
    }
}
