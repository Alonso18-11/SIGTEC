package com.softcorp.sigtec.feature.incidencias.domain

import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import java.text.Normalizer

// RF-42/RF-48/RF-51/RF-55: «INC-0124», o esto mientras la sincronización no le asigne número.
const val POR_SINCRONIZAR = "Por sincronizar"

/** «INC-0124», o «Por sincronizar» mientras la sincronización no le asigne número. */
fun Incidencia.codigoParaMostrar(): String = codigoVisible ?: POR_SINCRONIZAR

// RF-44: texto del filtro de técnico, y de la tarjeta, cuando no hay nadie asignado.
const val SIN_TECNICO = "Sin técnico"

data class FiltrosIncidencias(
    val texto: String = "",
    val marca: MarcaSeguimiento? = null,
    val tecnico: String? = null,          // nombre del técnico, o SIN_TECNICO
    val equipo: String? = null
) {
    val activos: Boolean
        get() = texto.isNotBlank() || marca != null || tecnico != null || equipo != null
}

/**
 * RF-43/RF-44: búsqueda de texto (sin mayúsculas ni tildes, por código visible/«Por sincronizar»,
 * equipo o descripción) y filtros combinables de marca, técnico (incluido "Sin técnico") y equipo.
 * Función pura: no toca Room, Firestore ni permisos; el ViewModel solo la invoca sobre los datos
 * que ya tiene cargados. No es un caso de uso con Resultado<T> porque no hay ningún camino de
 * error que validar contra el dominio, solo una transformación de una lista ya en memoria.
 */
fun List<Incidencia>.filtrar(f: FiltrosIncidencias): List<Incidencia> {
    val buscado = f.texto.normalizado()
    return filter { inc ->
        (buscado.isEmpty() || inc.coincideCon(buscado)) &&
                (f.marca == null || inc.marca == f.marca) &&
                (f.tecnico == null || (inc.tecnicoAsignadoNombre ?: SIN_TECNICO) == f.tecnico) &&
                (f.equipo == null || inc.codigoEquipo == f.equipo)
    }
}

// Código INC («124» también encuentra INC-0124), equipo o problema
private fun Incidencia.coincideCon(buscado: String): Boolean =
    listOf(codigoParaMostrar(), codigoEquipo, descripcion).any { it.normalizado().contains(buscado) }

// Sin mayúsculas ni tildes: «despues» encuentra «después»
private fun String.normalizado(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(MARCAS_DIACRITICAS, "").lowercase()

private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")
