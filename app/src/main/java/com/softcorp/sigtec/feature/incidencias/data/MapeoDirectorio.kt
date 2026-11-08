package com.softcorp.sigtec.feature.incidencias.data

import com.softcorp.sigtec.feature.incidencias.domain.Colaborador
import com.softcorp.sigtec.feature.incidencias.domain.ErrorDirectorio
import retrofit2.HttpException

// HU-12 (plan 3.2): traduce lo que responde el directorio al dominio. Sin Android, se prueba en la JVM.

/**
 * RF-1205: los colaboradores cuyo equipo es exactamente [codigo], sin distinguir mayúsculas ni espacios en los
 * extremos, y con nombre (RF-1211: un nombre vacío cuenta como «sin responsable»). El servidor de prueba busca por
 * coincidencia parcial, así que `PC-CONT-01` también trae a `PC-CONT-014`.
 */
internal fun List<ColaboradorDto>.coincidenciasExactas(codigo: String): List<Colaborador> {
    val buscado = codigo.trim()
    return filter { it.equipo.trim().equals(buscado, ignoreCase = true) && it.nombre.isNotBlank() }
        .map { Colaborador(nombre = it.nombre.trim(), area = it.area.trim(), anexo = it.anexo?.trim()?.ifEmpty { null }) }
}

/** RF-1211 y RF-1212: MockAPI responde 404 cuando nada coincide; cualquier otra falla es «sin respuesta». */
internal fun errorDeDirectorio(e: Throwable): ErrorDirectorio =
    if (e is HttpException && e.code() == 404) ErrorDirectorio.NO_ENCONTRADO else ErrorDirectorio.SIN_RESPUESTA
