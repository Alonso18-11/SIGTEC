package com.softcorp.sigtec.feature.ia.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.SugerenciaIA
import com.softcorp.sigtec.core.domain.model.TipoSugerencia
import com.softcorp.sigtec.feature.ia.domain.SugerenciaIARepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Doble en memoria de [SugerenciaIARepository]. Con [fallar] cada operación devuelve un error desconocido. */
class SugerenciaIARepositoryFalso : SugerenciaIARepository {

    private val _sugerencias = MutableStateFlow<List<SugerenciaIA>>(emptyList())
    val sugerencias: List<SugerenciaIA> get() = _sugerencias.value

    var fallar = false

    fun cargar(vararg lista: SugerenciaIA) {
        _sugerencias.value = lista.toList()
    }

    private fun masRecienteSinDecidir(lista: List<SugerenciaIA>, incidenciaId: String, tipo: TipoSugerencia) =
        lista.filter { it.incidenciaId == incidenciaId && it.tipo == tipo && it.aceptada == null }
            .maxByOrNull { it.generadaEn }

    override fun observarSinDecidir(incidenciaId: String, tipo: TipoSugerencia): Flow<SugerenciaIA?> =
        _sugerencias.map { masRecienteSinDecidir(it, incidenciaId, tipo) }

    override suspend fun sinDecidir(incidenciaId: String, tipo: TipoSugerencia): Resultado<SugerenciaIA?> =
        if (fallar) error() else Resultado.Exito(masRecienteSinDecidir(sugerencias, incidenciaId, tipo))

    override suspend fun contar(incidenciaId: String, tipo: TipoSugerencia): Resultado<Int> =
        if (fallar) error() else Resultado.Exito(sugerencias.count { it.incidenciaId == incidenciaId && it.tipo == tipo })

    override suspend fun guardar(sugerencia: SugerenciaIA): Resultado<Unit> {
        if (fallar) return error()
        _sugerencias.value = sugerencias.filterNot { it.id == sugerencia.id } + sugerencia
        return Resultado.Exito(Unit)
    }

    override suspend fun decidir(id: String, aceptada: Boolean, decididaPorId: String, ahora: Instant): Resultado<Unit> {
        if (fallar) return error()
        _sugerencias.value = sugerencias.map {
            if (it.id == id && it.aceptada == null) it.copy(aceptada = aceptada, decididaPorId = decididaPorId, decididaEn = ahora)
            else it
        }
        return Resultado.Exito(Unit)
    }

    private fun error() = Resultado.Error(ErrorDominio.Desconocido("falla de prueba"))
}
