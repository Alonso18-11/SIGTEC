package com.softcorp.sigtec.feature.indicadores.data

import com.softcorp.sigtec.core.domain.Resultado
import com.softcorp.sigtec.feature.indicadores.domain.DatosIndicadores
import com.softcorp.sigtec.feature.indicadores.domain.IndicadoresRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/** Doble de prueba del tablero: los datos se cambian a mano, como si llegaran por la sincronización. */
class IndicadoresRepositoryFalso(
    iniciales: DatosIndicadores = DatosIndicadores(emptyList(), emptyList(), emptyList(), emptyMap(), emptyList())
) : IndicadoresRepository {
    val datos = MutableStateFlow(iniciales)
    override fun observarDatos(): Flow<DatosIndicadores> = datos

    /** HU-18: las tareas anotadas, por tipo, con su último instante. */
    val tareas = mutableMapOf<String, Instant>()

    override suspend fun ultimaTareaExitosa(tipo: String): Instant? = tareas[tipo]

    override suspend fun registrarTarea(tipo: String, ahora: Instant): Resultado<Unit> {
        tareas[tipo] = ahora
        return Resultado.Exito(Unit)
    }
}
