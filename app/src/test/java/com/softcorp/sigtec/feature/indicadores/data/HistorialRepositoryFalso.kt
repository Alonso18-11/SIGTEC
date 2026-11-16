package com.softcorp.sigtec.feature.indicadores.data

import com.softcorp.sigtec.feature.indicadores.domain.DatosHistorial
import com.softcorp.sigtec.feature.indicadores.domain.HistorialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Doble de prueba del historial: los datos se cambian a mano, como si llegaran por la sincronización. Entrega a cada
 * código solo sus incidencias y anota qué códigos se pidieron.
 */
class HistorialRepositoryFalso(
    iniciales: DatosHistorial = DatosHistorial(null, emptyList(), emptyList(), emptyList())
) : HistorialRepository {
    val datos = MutableStateFlow(iniciales)
    val codigosPedidos = mutableListOf<String>()

    override fun observarDatos(codigo: String): Flow<DatosHistorial> {
        codigosPedidos += codigo
        return datos.map { d ->
            d.copy(
                equipo = d.equipo?.takeIf { it.codigo == codigo },
                incidencias = d.incidencias.filter { it.codigoEquipo == codigo }
            )
        }
    }
}
