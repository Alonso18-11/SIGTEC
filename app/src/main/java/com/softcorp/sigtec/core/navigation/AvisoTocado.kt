package com.softcorp.sigtec.core.navigation

import com.softcorp.sigtec.core.domain.model.EstadoAcceso

/** El aviso que la persona tocó: qué incidencia abrir y a quién se le avisó (HU-07). */
data class AvisoTocado(val incidenciaId: String, val usuarioId: String)

sealed interface DestinoDelAviso {
    data class Abrir(val incidenciaId: String) : DestinoDelAviso
    /** Todavía no se sabe quién entra (sin sesión o bloqueada): se decide cuando la sesión quede activa. */
    data object Esperar : DestinoDelAviso
    data object Descartar : DestinoDelAviso
}

/**
 * RF-711 a RF-713: solo se abre el detalle con la sesión activa de quien recibió el aviso, así nunca
 * se salta el login ni la huella. Con la sesión de otra persona se descarta.
 */
fun destinoDelAviso(estado: EstadoAcceso, aviso: AvisoTocado): DestinoDelAviso = when (estado) {
    is EstadoAcceso.Activa ->
        if (estado.usuario.id == aviso.usuarioId) DestinoDelAviso.Abrir(aviso.incidenciaId)
        else DestinoDelAviso.Descartar
    is EstadoAcceso.Bloqueada, EstadoAcceso.SinSesion -> DestinoDelAviso.Esperar
}
