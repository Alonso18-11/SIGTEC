package com.softcorp.sigtec.feature.atencion.data

import com.softcorp.sigtec.core.domain.model.EstadoEntrega
import com.softcorp.sigtec.core.domain.model.SolicitudRepuesto
import com.softcorp.sigtec.feature.atencion.domain.ErrorLogistica
import com.softcorp.sigtec.feature.atencion.domain.EstadoEnLogistica
import com.softcorp.sigtec.feature.atencion.domain.LogisticaRepository
import com.softcorp.sigtec.feature.atencion.domain.PiezaLogistica
import com.softcorp.sigtec.feature.atencion.domain.ResultadoLogistica

/**
 * HU-11: Logística en memoria, con algunas piezas del servidor de prueba. Con [fallaBusqueda], [fallaRegistro] o
 * [fallaConsulta] simula cada error; [estados] es lo que responde al consultar cada número.
 */
class LogisticaRepositoryFalso(
    var piezas: List<PiezaLogistica> = listOf(
        PiezaLogistica("1", "Fuente de poder ATX 500 W", stock = 3, diasEnvio = 1),
        PiezaLogistica("3", "Disco SSD 480 GB", stock = 2, diasEnvio = 2),
        PiezaLogistica("6", "Mouse óptico USB", stock = 0, diasEnvio = 3)
    )
) : LogisticaRepository {

    var fallaBusqueda: ErrorLogistica? = null
    var fallaRegistro: ErrorLogistica? = null
    var fallaConsulta: ErrorLogistica? = null

    /** Lo que responde `consultar` por número; si falta, la solicitud sigue por despachar. */
    val estados = mutableMapOf<String, EstadoEnLogistica>()

    val busquedas = mutableListOf<String>()
    val registradas = mutableListOf<SolicitudRepuesto>()
    private var siguienteNumero = 100

    override suspend fun buscarPiezas(texto: String): ResultadoLogistica<List<PiezaLogistica>> {
        busquedas += texto
        fallaBusqueda?.let { return ResultadoLogistica.Fallo(it) }
        return ResultadoLogistica.Exito(piezas.filter { it.nombre.contains(texto.trim(), ignoreCase = true) })
    }

    override suspend fun registrar(
        solicitud: SolicitudRepuesto,
        codigoIncidencia: String,
        codigoEquipo: String
    ): ResultadoLogistica<String> {
        fallaRegistro?.let { return ResultadoLogistica.Fallo(it) }
        registradas += solicitud
        return ResultadoLogistica.Exito((siguienteNumero++).toString())
    }

    override suspend fun consultar(numero: String): ResultadoLogistica<EstadoEnLogistica> {
        fallaConsulta?.let { return ResultadoLogistica.Fallo(it) }
        return ResultadoLogistica.Exito(
            estados[numero] ?: EstadoEnLogistica(EstadoEntrega.POR_DESPACHAR, null, null, null)
        )
    }
}
