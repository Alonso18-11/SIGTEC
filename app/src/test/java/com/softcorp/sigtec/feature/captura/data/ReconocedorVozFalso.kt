package com.softcorp.sigtec.feature.captura.data

import com.softcorp.sigtec.feature.captura.domain.EventoDictado
import com.softcorp.sigtec.feature.captura.domain.ReconocedorVoz
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformWhile

/**
 * HU-10: reconocedor en memoria. La prueba manda los eventos con [emitir]; cada escucha termina con un `Final` o un
 * `Error`, igual que el real. Cuenta las escuchas, las veces que se pidió detener y las que se canceló.
 */
class ReconocedorVozFalso(override var disponible: Boolean = true) : ReconocedorVoz {

    private var eventos = Channel<EventoDictado>(Channel.UNLIMITED)

    var escuchas: Int = 0
        private set
    var detenciones: Int = 0
        private set
    var cancelaciones: Int = 0
        private set

    /** `true` mientras alguien recolecta una escucha que no terminó. */
    var escuchando: Boolean = false
        private set

    override fun escuchar(): Flow<EventoDictado> = flow {
        escuchas++
        eventos = Channel(Channel.UNLIMITED)
        escuchando = true
        var termino = false
        try {
            emitAll(
                eventos.receiveAsFlow().transformWhile { evento ->
                    emit(evento)
                    val fin = evento is EventoDictado.Final || evento is EventoDictado.Error
                    if (fin) termino = true
                    !fin
                }
            )
        } finally {
            escuchando = false
            if (!termino) cancelaciones++
        }
    }

    override fun detener() {
        detenciones++
    }

    fun emitir(vararg nuevos: EventoDictado) {
        nuevos.forEach { eventos.trySend(it) }
    }
}
