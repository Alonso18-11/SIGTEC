package com.softcorp.sigtec.feature.diccionario.data

import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.feature.diccionario.domain.DiccionarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Doble de prueba del diccionario: guarda en memoria y puede fallar a pedido. */
class DiccionarioRepositoryFalso(iniciales: List<FallaDiccionario> = emptyList()) : DiccionarioRepository {

    private val estado = MutableStateFlow(iniciales)
    val fallas: List<FallaDiccionario> get() = estado.value
    var fallarAlGuardar = false

    override fun observarTodas(): Flow<List<FallaDiccionario>> = estado.map { lista -> lista.sortedBy { it.titulo } }

    override fun existeConOrigen(incidenciaId: String): Flow<Boolean> =
        estado.map { lista -> lista.any { it.incidenciaOrigenId == incidenciaId } }

    override suspend fun guardar(falla: FallaDiccionario): Resultado<Unit> {
        if (fallarAlGuardar) return Resultado.Error(ErrorDominio.Desconocido("falla de prueba"))
        estado.value = estado.value.filterNot { it.id == falla.id } + falla
        return Resultado.Exito(Unit)
    }

    /** Simula que la sincronización trajo fallas nuevas. */
    fun agregar(vararg nuevas: FallaDiccionario) {
        estado.value = estado.value + nuevas
    }
}
