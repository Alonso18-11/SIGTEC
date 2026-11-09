package com.softcorp.sigtec.feature.indicadores.data

import com.softcorp.sigtec.feature.indicadores.domain.DatosIndicadores
import com.softcorp.sigtec.feature.indicadores.domain.IndicadoresRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Doble de prueba del tablero: los datos se cambian a mano, como si llegaran por la sincronización. */
class IndicadoresRepositoryFalso(
    iniciales: DatosIndicadores = DatosIndicadores(emptyList(), emptyList(), emptyList(), emptyMap(), emptyList())
) : IndicadoresRepository {
    val datos = MutableStateFlow(iniciales)
    override fun observarDatos(): Flow<DatosIndicadores> = datos
}
