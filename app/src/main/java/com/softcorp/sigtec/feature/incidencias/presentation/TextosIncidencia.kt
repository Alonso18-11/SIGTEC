package com.softcorp.sigtec.feature.incidencias.presentation

import com.softcorp.sigtec.core.domain.model.Incidencia

/** «INC-0124», o «Por sincronizar» mientras la sincronización no le asigne número. */
fun Incidencia.codigoParaMostrar(): String = codigoVisible ?: POR_SINCRONIZAR

const val POR_SINCRONIZAR = "Por sincronizar"
